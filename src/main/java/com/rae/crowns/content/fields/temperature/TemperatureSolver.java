package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.AbstractDataLayer;
import com.rae.crowns.content.fields.util.AbstractMatrixPhysicsSolver;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.formicapi.fondation.math.operators.PaddedCSRMatrix;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lwjgl.system.NonnullDefault;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Implicit finite-difference temperature diffusion solver.
 *
 * <p>Discretises the heat equation:
 * <pre>
 *   C * (T_next - T_current) / dt = k∇²T_next + β*resilience*(T_default - T_next)
 * </pre>
 * Rearranged into the linear system {@code A * T_next = T_current + b_source}.
 *
 * <p>Only implements field I/O, dynamic data updates, and the per-voxel
 * coefficient formula. All matrix management is handled by the base class.
 */
@NonnullDefault
public final class TemperatureSolver extends AbstractMatrixPhysicsSolver<TemperatureSolver.ThermalMatrix> {

    public static float DT       = 1 / 20f;
    public static float CAPACITY = 3e4f;

    private static final int TEMPERATURE = 0;
    private static final int DEFAULT_TEMPERATURE = 1;
    private static final int CONDUCTION = 2;
    private static final int RESILIENCE = 3;

    double EPSILON = 1e-1f;
    double gamma = (double) DT / CAPACITY;          // diffusion time-scale
    double beta  = 1e5 * DT / CAPACITY;          // resilience time-scale

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    @Override
    protected float getTimeStep() { return DT; }

    @Override
    protected DataLayerType[] getRequiredLayers() {
        return new DataLayerType[]{
                DataLayerType.TEMPERATURE,
                DataLayerType.DEFAULT_TEMPERATURE,
                DataLayerType.CONDUCTION,
                DataLayerType.RESILIENCE
        };
    }

    // -------------------------------------------------------------------------
    // Matrix factory and cache
    // -------------------------------------------------------------------------

    @Override
    protected ThermalMatrix createMatrix(LongSet sections, Long2IntMap sectionToIndex, int totalSize) {
        return new ThermalMatrix(sections, sectionToIndex, totalSize);
    }

    @Override
    protected @Nullable ThermalMatrix getCachedMatrix(PhysicsWorldData data) {
        return (ThermalMatrix) data.getCachedMatrix(this);
    }

    @Override
    protected void setCachedMatrix(PhysicsWorldData data, ThermalMatrix matrix) {
        data.setCachedMatrix(this, matrix);
    }

    // -------------------------------------------------------------------------
    // Field I/O
    // -------------------------------------------------------------------------

    @Override
    protected void extractFieldValues(ThermalMatrix matrix, PhysicsWorldData data) {
        for (long packedSection : matrix.sections()) {
            int start = matrix.sectionToIndex().get(packedSection);
            if (start < 0) continue;

            TemperatureDataLayer layer = (TemperatureDataLayer) data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            for (short localIdx = 0; localIdx < 4096; localIdx++)
                        matrix.T_current[start + localIdx] = layer.getDirect(localIdx);
        }
    }

    @Override
    protected void writeBackFieldValues(ThermalMatrix matrix, PhysicsWorldData data) {
        for (long packedSection : matrix.sections()) {
            int start = matrix.sectionToIndex().get(packedSection);
            if (start < 0) continue;

            TemperatureDataLayer layer = (TemperatureDataLayer) data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            boolean anyChanged = false;

            boolean touchMinX = false;
            boolean touchMaxX = false;
            boolean touchMinY = false;
            boolean touchMaxY = false;
            boolean touchMinZ = false;
            boolean touchMaxZ = false;

            for (short localIdx = 0; localIdx < 4096; localIdx++) {
                int idx = start + localIdx;

                if (Math.abs(matrix.T_next[idx] - matrix.T_current[idx]) > EPSILON) {
                    anyChanged = true;

                    if (IS_BOUNDARY[localIdx]) {
                        int x = localIdx & 15;
                        int z = (localIdx >> 4) & 15;
                        int y = (localIdx >> 8) & 15;

                        if (x == 0) touchMinX = true;
                        if (x == 15) touchMaxX = true;
                        if (y == 0) touchMinY = true;
                        if (y == 15) touchMaxY = true;
                        if (z == 0) touchMinZ = true;
                        if (z == 15) touchMaxZ = true;
                    }
                }

                layer.setDirect(localIdx, (float) matrix.T_next[idx]);
            }

            if (anyChanged) {
                data.setNeedTicking(packedSection);

                int sx = SectionPos.x(packedSection);
                int sy = SectionPos.y(packedSection);
                int sz = SectionPos.z(packedSection);

                if (touchMinX) data.setNeedTicking(SectionPos.asLong(sx - 1, sy, sz));
                if (touchMaxX) data.setNeedTicking(SectionPos.asLong(sx + 1, sy, sz));

                if (touchMinY) data.setNeedTicking(SectionPos.asLong(sx, sy - 1, sz));
                if (touchMaxY) data.setNeedTicking(SectionPos.asLong(sx, sy + 1, sz));

                if (touchMinZ) data.setNeedTicking(SectionPos.asLong(sx, sy, sz - 1));
                if (touchMaxZ) data.setNeedTicking(SectionPos.asLong(sx, sy, sz + 1));
            } else {
                data.setNoTicking(packedSection);
            }
        }

        // Dynamic sources receive the delta the solver computed for their voxel,
        // expressed as addTemperature(T_next - T_current).
        data.getDynamicData().forEach((key, source) -> {
            if (source instanceof BlockEntity be && be.isRemoved()) return;

            BlockPos pos           = BlockPos.of(key);
            long     packedSection = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            int      start         = matrix.sectionToIndex().get(packedSection);
            if (start < 0) return;

            data.setNeedTicking(packedSection);

            int idx    = start + index3DTo1D(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
            float dT   = (float) (matrix.T_next[idx] - matrix.T_current[idx]);
            source.addTemperature(dT);
        });
    }

    // -------------------------------------------------------------------------
    // RHS assembly: A*T_next = T_current + b_source
    // -------------------------------------------------------------------------

    /**
     * Combines T_current and the source term into the RHS passed to the solver.
     */
    @Override
    protected double[] buildRhs(ThermalMatrix matrix) {
        double[] rhs = matrix.cgRhs;
        double[] src = matrix.sourceVector();
        double[] t   = matrix.T_current;
        for (int i = 0; i < rhs.length; i++) rhs[i] = t[i] + src[i];
        return rhs;
    }

    // -------------------------------------------------------------------------
    // Physics: implicit diffusion + resilience
    // -------------------------------------------------------------------------


    double[] values = new double[7];//reusable values when building the row, expecting only one row at a time.
    int[] cols = new int[7];
    @Override
    protected void buildVoxelRow(
            short x, short y, short z,
            int globalIdx,
            long packedSection,
            int sectionStartIdx,
            PhysicsWorldData data,
            NeighborCache neighbors,
            PaddedCSRMatrix assemblyMatrix,
            double[] sourceVector
    ) {
        ConductionDataLayer  condLayer    = (ConductionDataLayer) neighbors.getLayer(6, CONDUCTION);
        ResilienceDataLayer  resLayer     = (ResilienceDataLayer) neighbors.getLayer(6, RESILIENCE);
        TemperatureDataLayer defaultLayer = (TemperatureDataLayer) neighbors.getLayer(6, DEFAULT_TEMPERATURE);

        // Degenerate case — no data: identity row so the voxel just keeps its value
        if (condLayer == null || resLayer == null || defaultLayer == null) {
            values[0] = 1.0;
            cols[0]   = globalIdx;
            assemblyMatrix.setRow(globalIdx, values, cols, 1);
            sourceVector[globalIdx] = 0;
            return;
        }
        short localIdx = index3DTo1D(x, y, z);
        float selfCond   = condLayer.getDirect(localIdx);
        float res        = resLayer.getDirect(localIdx);
        float defaultTemp = defaultLayer.getDirect(localIdx);

        // Resilience source: pulls voxel toward its default temperature
        sourceVector[globalIdx] = res * beta * defaultTemp;

        // Diagonal starts at 1 (implicit identity from T_current) and loses beta
        double diag = 1.0 + res * beta;

        int count = 0;


        for (byte[] off : NEIGHBOR_OFFSETS) {
            short nx  = (short) (x + off[0]), ny = (short) (y + off[1]), nz = (short) (z + off[2]);
            short lnx = (short) (nx & 15), lny = (short) (ny & 15), lnz = (short) (nz & 15);

            int nidx = NeighborCache.getIndex(nx, ny, nz);

            ConductionDataLayer nbCond = (ConductionDataLayer) neighbors.getLayer(nidx, CONDUCTION);
            if (nbCond == null) continue;

            float neighborCond = nbCond.get(lnx, lny, lnz);

            // Harmonic mean of the two conductivities
            double k_eff = (selfCond <= 0 || neighborCond <= 0) ? 0.0
                    : 2.0 * selfCond * neighborCond / (selfCond + neighborCond);

            double coeff = gamma * k_eff;//(1.0 - res) * gamma * k_eff; if we remove the res on diffusion it's symmetrical

            if (neighbors.isInMatrix(nidx)) {
                // Interior
                values[count] = -coeff;
                cols[count] = neighbors.globalIndex(nidx) + index3DTo1D(lnx, lny, lnz);
                //assemblyMatrix.set(globalIdx, neighbors.globalIndex(nidx) + index3DTo1D(lnx, lny, lnz), -coeff);
                diag += coeff;
                count++;

            } else {
                //TODO check the validity of this
                //Boundary: known temperature folds into the RHS
                TemperatureDataLayer nbTemp = (TemperatureDataLayer) neighbors.getLayer(nidx, TEMPERATURE);
                if (nbTemp != null) {
                    sourceVector[globalIdx] += coeff * nbTemp.get(lnx, lny, lnz);
                } else {
                    sourceVector[globalIdx] += coeff * 300;//300K is default
                }
                diag += coeff;
            }
        }
        values[count] = diag;
        cols[count] = globalIdx;
        count++;
        assemblyMatrix.setRow(globalIdx, values, cols, count);
    }

    // -------------------------------------------------------------------------
    // Dynamic data (block entities that act as heat sources)
    // -------------------------------------------------------------------------

    @Override
    protected void updateDynamicData(PhysicsWorldData data) {
        List<BlockPos> toStamp = new ArrayList<>();

        data.getDynamicData().forEach((key, source) -> {
            if (source instanceof BlockEntity be && be.isRemoved()) return;

            BlockPos pos = BlockPos.of(key);
            int sx = pos.getX() >> 4;
            int sy = pos.getY() >> 4;
            int sz = pos.getZ() >> 4;
            long packedSection = SectionPos.asLong(sx, sy, sz);

            AbstractDataLayer    tempLayer    = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            AbstractDataLayer defaultLayer = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
            AbstractDataLayer  condLayer    = data.getLayer(packedSection, DataLayerType.CONDUCTION);
            AbstractDataLayer  resLayer     = data.getLayer(packedSection, DataLayerType.RESILIENCE);

            if (tempLayer == null || defaultLayer == null || condLayer == null || resLayer == null) return;

            int lx = pos.getX() & 15;
            int ly = pos.getY() & 15;
            int lz = pos.getZ() & 15;

            float temp = source.getTemperature();
            short localIdx = index3DTo1D(lx, ly, lz);
            tempLayer.setDirect(localIdx, temp);
            defaultLayer.setDirect(localIdx, temp);
            resLayer.setDirect(localIdx, 0f);
            condLayer.setDirect(localIdx, source.getThermalConductivity());

            data.setNeedTicking(packedSection);
            toStamp.add(pos);
        });

        if (!toStamp.isEmpty()) stampVoxels(toStamp, data);
    }

    // -------------------------------------------------------------------------
    // Concrete matrix type
    // -------------------------------------------------------------------------

    /**
     * Thermal matrix carrying the two temperature state vectors.
     *
     * <p>{@code T_current} is populated each tick from world data before the solve.
     * {@code T_next} receives the solution and is written back afterward.
     */
    public static class ThermalMatrix extends PhysicsMatrix {
        double[] T_current;
        double[] T_next;

        public ThermalMatrix(LongSet sections, Long2IntMap sectionToIndex, int size) {
            super(sections, sectionToIndex, size, 7);
            T_current = new double[size];
            T_next    = new double[size];
        }

        @Override
        protected void onGrow(int newSize) {
            T_current = Arrays.copyOf(T_current, newSize);
            T_next    = Arrays.copyOf(T_next,    newSize);
        }

        @Override
        public void setSolution(double[] solution) {
            System.arraycopy(solution, 0, T_next, 0, solution.length);
        }

        @Override
        public double[] getInitX() {
            return T_next;
        }

        @Override
        protected void copyFieldArrays(int srcStart, int dstStart, int count) {
            System.arraycopy(T_current, srcStart, T_current, dstStart, count);
            System.arraycopy(T_next,    srcStart, T_next,    dstStart, count);
        }

        @Override
        protected void onShrink(int newSize) {
            T_current = Arrays.copyOf(T_current, newSize);
            T_next    = Arrays.copyOf(T_next,    newSize);
        }
    }
}