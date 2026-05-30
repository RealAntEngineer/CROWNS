package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.AbstractMatrixPhysicsSolver;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.formicapi.fondation.math.solvers.LeastSquare;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Concrete implementation of matrix-based temperature diffusion solver.
 * Extends AbstractMatrixPhysicsSolver with temperature-specific physics.
 */
public final class TemperatureSolver extends AbstractMatrixPhysicsSolver<TemperatureSolver.ThermalMatrix> {

    public static float DT = 1 / 20f;
    public static float CAPACITY = 3e4f;

    @Override
    protected float getTimeStep() {
        return DT;
    }

    @Override
    protected DataLayerType[] getRequiredLayers() {
        return new DataLayerType[]{
            DataLayerType.TEMPERATURE,
            DataLayerType.DEFAULT_TEMPERATURE,
            DataLayerType.CONDUCTION,
            DataLayerType.RESILIENCE
        };
    }

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

    @Override
    protected void extractFieldValues(ThermalMatrix matrix, PhysicsWorldData data) {
        for (long packedSection : matrix.sections()) {
            int sectionStartIdx = matrix.sectionToIndex().get(packedSection);
            if (sectionStartIdx < 0) continue;

            TemperatureDataLayer tempLayer = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            if (tempLayer == null) continue;

            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int localIdx = index3DTo1D(x, y, z);
                        int globalIdx = sectionStartIdx + localIdx;
                        matrix.T_current[globalIdx] = tempLayer.get(x, y, z);
                    }
                }
            }
        }
    }

    @Override
    protected void writeBackFieldValues(ThermalMatrix matrix, PhysicsWorldData data) {
        for (long packedSection : matrix.sections()) {
            int sectionStartIdx = matrix.sectionToIndex().get(packedSection);
            if (sectionStartIdx < 0) continue;

            TemperatureDataLayer tempLayer = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            if (tempLayer == null) continue;

            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int localIdx = index3DTo1D(x, y, z);
                        int globalIdx = sectionStartIdx + localIdx;
                        tempLayer.set(x, y, z, (float) matrix.T_next[globalIdx]);
                    }
                }
            }
        }
    }

    @Override
    protected double buildVoxelRow(
            int x, int y, int z,
            int globalIdx,
            SectionPos sectionPos,
            long packedSection,
            int sectionStartIdx,
            PhysicsWorldData data,
            NeighborCache neighbors,
            Int2DoubleMap row,
            double[] sourceVector
    ) {
        // Load voxel properties
        ConductionDataLayer  condLayer        = neighbors.getLayer(packedSection, DataLayerType.CONDUCTION);
        ResilienceDataLayer  resLayer         = neighbors.getLayer(packedSection, DataLayerType.RESILIENCE);
        TemperatureDataLayer defaultTempLayer = neighbors.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);

        if (condLayer == null || resLayer == null || defaultTempLayer == null) {
            return 1.0; // Identity row if data missing
        }

        float selfCond = condLayer.get(x, y, z);
        float res = resLayer.get(x, y, z);
        float defaultTemp = defaultTempLayer.get(x, y, z);

        double gamma = DT / CAPACITY;
        double beta = 1000.0 * DT / CAPACITY;

        // Source term: resilience pulling toward default temperature
        sourceVector[globalIdx] = res * beta * defaultTemp;

        // Diagonal coefficient starts with identity and resilience penalty
        double diagCoeff = 1.0 - res * beta;

        // Process all 6 neighbors for diffusion
        for (int[] offset : NEIGHBOR_OFFSETS) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            int nz = z + offset[2];

            NeighborInfo neighbor = getNeighborInfo(nx, ny, nz, sectionPos, packedSection, sectionStartIdx, neighbors);
            if (neighbor == null) continue;

            // Get neighbor conductivity
            ConductionDataLayer nCondLayer = neighbors.getLayer(neighbor.section(), DataLayerType.CONDUCTION);
            if (nCondLayer == null) continue;

            float neighborCond = nCondLayer.get(neighbor.localX(), neighbor.localY(), neighbor.localZ());

            // Harmonic mean for effective conductivity
            double k_eff = (selfCond <= 0 || neighborCond <= 0) ? 0.0 :
                           2.0 * selfCond * neighborCond / (selfCond + neighborCond);

            double condCoeff = (1.0 - res) * gamma * k_eff;

            if (neighbor.isInMatrix()) {
                // Neighbor is in the system - add off-diagonal term
                row.put(neighbor.globalIndex(), condCoeff);
                diagCoeff -= condCoeff;
            } else {
                // Boundary condition - neighbor not in system
                TemperatureDataLayer nTempLayer = neighbors.getLayer(neighbor.section(), DataLayerType.TEMPERATURE);
                if (nTempLayer != null) {
                    float boundaryTemp = nTempLayer.get(neighbor.localX(), neighbor.localY(), neighbor.localZ());
                    sourceVector[globalIdx] += condCoeff * boundaryTemp;
                }
            }
        }

        return diagCoeff;
    }

    @Override
    protected void updateDynamicData(@NotNull PhysicsWorldData data) {
        List<BlockPos> toStamp = new ArrayList<>();

        data.getDynamicData().forEach((key, value) -> {
            BlockPos pos = BlockPos.of(key);

            if (value instanceof BlockEntity blockEntity && blockEntity.isRemoved()) {
                return;
            }

            int sx = pos.getX() >> 4;
            int sy = pos.getY() >> 4;
            int sz = pos.getZ() >> 4;
            long packedSection = SectionPos.asLong(sx, sy, sz);

            TemperatureDataLayer temperatureData = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            TemperatureDataLayer defaultTemperatureData = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
            ConductionDataLayer conductionData = data.getLayer(packedSection, DataLayerType.CONDUCTION);
            ResilienceDataLayer resilienceData = data.getLayer(packedSection, DataLayerType.RESILIENCE);

            if (temperatureData == null || defaultTemperatureData == null ||
                    conductionData == null || resilienceData == null) {
                return;
            }

            int lx = pos.getX() & 15;
            int ly = pos.getY() & 15;
            int lz = pos.getZ() & 15;

            float temp = value.getTemperature();

            temperatureData.set(lx, ly, lz, temp);
            defaultTemperatureData.set(lx, ly, lz, temp);
            resilienceData.set(lx, ly, lz, 0);
            conductionData.set(lx, ly, lz, value.getThermalConductivity());

            data.setDirty(packedSection);

            toStamp.add(pos);
        });

        if (!toStamp.isEmpty()) {
            stampVoxels(toStamp, data);
        }
    }

    @Override
    public void tick(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        data.resetTicked();
        updateDynamicData(data);

        ThermalMatrix thermalMatrix = getOrBuildMatrix(tickingSections, data);
        if (thermalMatrix == null || thermalMatrix.size() == 0) {
            return;
        }

        extractFieldValues(thermalMatrix, data);

        // Build right-hand side: T_current + source term
        double[] rhs = Arrays.copyOf(thermalMatrix.T_current, thermalMatrix.size());
        for (int i = 0; i < thermalMatrix.size(); i++) {
            rhs[i] += thermalMatrix.b[i];
        }

        // Solve: A * T_next = rhs
        double[] solution = LeastSquare.solve(
                thermalMatrix.matrix(),
                rhs,
                getSolverMaxIterations(),
                getSolverTolerance()
        );

        System.arraycopy(solution, 0, thermalMatrix.T_next, 0, solution.length);

        writeBackFieldValues(thermalMatrix, data);
        data.setDirty();
    }

    /**
     * Thermal matrix with temperature-specific state vectors
     */
    public static class ThermalMatrix extends PhysicsMatrix {
        private final double[] T_current;
        private final double[] T_next;

        public ThermalMatrix(LongSet sections, Long2IntMap sectionToIndex, int size) {
            super(sections, sectionToIndex, size);
            this.T_current = new double[size];
            this.T_next = new double[size];
        }

        @Override
        protected void onGrow(int newSize) {
            
        }

        @Override
        public void setSolution(double[] solution) {
            System.arraycopy(solution, 0, T_next, 0, solution.length);
        }

        public double[] getCurrentTemperatures() {
            return T_current;
        }

        public double[] getNextTemperatures() {
            return T_next;
        }
    }
}
