package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.AbstractMatrixPhysicsSolver;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.formicapi.fondation.math.operators.HashSparseMatrix;
import com.rae.formicapi.fondation.math.solvers.LeastSquare;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
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

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    @Override
    protected float getTimeStep() { return DT; }

    @Override
    protected DataLayerType<?>[] getRequiredLayers() {
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

            TemperatureDataLayer layer = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
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

            TemperatureDataLayer layer = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++)
                        layer.set(x, y, z, (float) matrix.T_next[start + index3DTo1D(x, y, z)]);
        }
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
    protected double buildVoxelRow(
            int x, int y, int z,
            int globalIdx,
            SectionPos sectionPos,
            long packedSection,
            int sectionStartIdx,
            PhysicsWorldData data,
            NeighborCache neighbors,
            HashSparseMatrix assemblyMatrix,
            double[] sourceVector
    ) {
        ConductionDataLayer  condLayer    = neighbors.getLayer(packedSection, DataLayerType.CONDUCTION);
        ResilienceDataLayer  resLayer     = neighbors.getLayer(packedSection, DataLayerType.RESILIENCE);
        TemperatureDataLayer defaultLayer = neighbors.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);

        // Degenerate case — no data: identity row so the voxel just keeps its value
        if (condLayer == null || resLayer == null || defaultLayer == null) return 1.0;

        float selfCond   = condLayer.get(x, y, z);
        float res        = resLayer.get(x, y, z);
        float defaultTemp = defaultLayer.get(x, y, z);

        double gamma = (double) DT / CAPACITY;          // diffusion time-scale
        double beta  = 1000.0 * DT / CAPACITY;          // resilience time-scale

        // Resilience source: pulls voxel toward its default temperature
        sourceVector[globalIdx] = res * beta * defaultTemp;

        // Diagonal starts at 1 (implicit identity from T_current) and loses beta
        double diag = 1.0 - res * beta;

        for (int[] off : NEIGHBOR_OFFSETS) {
            NeighborInfo nb = getNeighborInfo(
                    x + off[0], y + off[1], z + off[2],
                    sectionPos, packedSection, sectionStartIdx, neighbors);

            if (nb == null) continue;

            ConductionDataLayer nbCond = neighbors.getLayer(nb.section(), DataLayerType.CONDUCTION);
            if (nbCond == null) continue;

            float neighborCond = nbCond.get(nb.localX(), nb.localY(), nb.localZ());

            // Harmonic mean of the two conductivities
            double k_eff = (selfCond <= 0 || neighborCond <= 0) ? 0.0
                    : 2.0 * selfCond * neighborCond / (selfCond + neighborCond);

            double coeff = (1.0 - res) * gamma * k_eff;

            if (nb.isInMatrix()) {
                // Interior: off-diagonal coupling
                assemblyMatrix.set(globalIdx, nb.globalIndex(), coeff);
                diag -= coeff;
            } else {
                // Boundary: known temperature folds into the RHS
                TemperatureDataLayer nbTemp = neighbors.getLayer(nb.section(), DataLayerType.TEMPERATURE);
                if (nbTemp != null) {
                    sourceVector[globalIdx] += coeff * nbTemp.get(nb.localX(), nb.localY(), nb.localZ());
                }
            }
        }

        return diag;
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

            TemperatureDataLayer tempLayer    = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            TemperatureDataLayer defaultLayer = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
            ConductionDataLayer  condLayer    = data.getLayer(packedSection, DataLayerType.CONDUCTION);
            ResilienceDataLayer  resLayer     = data.getLayer(packedSection, DataLayerType.RESILIENCE);

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