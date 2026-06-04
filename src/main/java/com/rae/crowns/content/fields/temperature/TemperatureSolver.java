package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.AbstractDataLayer;
import com.rae.crowns.content.fields.util.AbstractMatrixPhysicsSolver;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

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
public final class TemperatureSolver extends AbstractMatrixPhysicsSolver<TemperatureSolver.ThermalMatrix> {

    public static float DT       = 1 / 20f;
    public static float CAPACITY = 3e4f;

    private static final int TEMPERATURE = 0;
    private static final int DEFAULT_TEMPERATURE = 1;
    private static final int CONDUCTION = 2;
    private static final int RESILIENCE = 3;


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
    protected ThermalMatrix getCachedMatrix(PhysicsWorldData data) {
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

            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++)
                        matrix.T_current[start + index3DTo1D(x, y, z)] = layer.get(x, y, z);
        }
    }

    @Override
    protected void writeBackFieldValues(ThermalMatrix matrix, PhysicsWorldData data) {
        for (long packedSection : matrix.sections()) {
            int start = matrix.sectionToIndex().get(packedSection);
            if (start < 0) continue;

            TemperatureDataLayer layer = (TemperatureDataLayer) data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++)
                        layer.set(x, y, z, (float) matrix.T_next[start + index3DTo1D(x, y, z)]);
        }

        // Dynamic sources receive the delta the solver computed for their voxel,
        // expressed as addTemperature(T_next - T_current).
        data.getDynamicData().forEach((key, source) -> {
            if (source instanceof BlockEntity be && be.isRemoved()) return;

            BlockPos pos           = BlockPos.of(key);
            long     packedSection = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            int      start         = matrix.sectionToIndex().get(packedSection);
            if (start < 0) return;

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
        double[] rhs = Arrays.copyOf(matrix.T_current, matrix.size());
        double[] src = matrix.sourceVector();
        for (int i = 0; i < rhs.length; i++) rhs[i] += src[i];
        return rhs;
    }

    // -------------------------------------------------------------------------
    // Physics: implicit diffusion + resilience
    // -------------------------------------------------------------------------

    @Override
    protected void buildVoxelRow(
            int x, int y, int z,
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
            assemblyMatrix.set(globalIdx, globalIdx, 1.0);
            return;
        }

        float selfCond   = condLayer.get(x, y, z);
        float res        = resLayer.get(x, y, z);
        float defaultTemp = defaultLayer.get(x, y, z);

        double gamma = (double) DT / CAPACITY;          // diffusion time-scale
        double beta  = 1000.0 * DT / CAPACITY;          // resilience time-scale

        // Resilience source: pulls voxel toward its default temperature
        sourceVector[globalIdx] = res * beta * defaultTemp;

        // Diagonal starts at 1 (implicit identity from T_current) and loses beta
        double diag = 1.0 + res * beta;

        int count = 0;
        double[] values = new double[7];
        int[] cols = new int[7];
        for (int[] off : NEIGHBOR_OFFSETS) {
            int nx = x + off[0], ny = y + off[1], nz = z + off[2];
            int lnx = nx & 15, lny = ny & 15, lnz = nz & 15;

            int nidx = NeighborCache.getIndex(nx, ny, nz);

            ConductionDataLayer nbCond = (ConductionDataLayer) neighbors.getLayer(nidx, CONDUCTION);
            if (nbCond == null) continue;

            float neighborCond = nbCond.get(lnx, lny, lnz);

            // Harmonic mean of the two conductivities
            double k_eff = (selfCond <= 0 || neighborCond <= 0) ? 0.0
                    : 2.0 * selfCond * neighborCond / (selfCond + neighborCond);

            double coeff = (1.0 - res) * gamma * k_eff;

            if (neighbors.isInMatrix(nidx)) {
                // Interior
                values[count] = -coeff;
                cols[count] = neighbors.globalIndex(nidx) + index3DTo1D(lnx, lny, lnz);
                //assemblyMatrix.set(globalIdx, neighbors.globalIndex(nidx) + index3DTo1D(lnx, lny, lnz), -coeff);
                diag += coeff;
                count++;

            } else {
                // Boundary: known temperature folds into the RHS
                TemperatureDataLayer nbTemp = (TemperatureDataLayer) neighbors.getLayer(nidx, TEMPERATURE);
                if (nbTemp != null) {
                    sourceVector[globalIdx] += coeff * nbTemp.get(lnx, lny, lnz);
                } else {
                    sourceVector[globalIdx] += coeff * 300;//300K is default
                }
            }
        }
        values[count] = diag;
        cols[count] = globalIdx;
        count++;
        assemblyMatrix.setRow(globalIdx, Arrays.copyOf(values, count), Arrays.copyOf(cols, count));
    }

    // -------------------------------------------------------------------------
    // Dynamic data (block entities that act as heat sources)
    // -------------------------------------------------------------------------

    @Override
    protected void updateDynamicData(@NotNull PhysicsWorldData data) {
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
            tempLayer.set(lx, ly, lz, temp);
            defaultLayer.set(lx, ly, lz, temp);
            resLayer.set(lx, ly, lz, 0f);
            condLayer.set(lx, ly, lz, source.getThermalConductivity());

            data.setDirty(packedSection);
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
            super(sections, sectionToIndex, size);
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
    }
}