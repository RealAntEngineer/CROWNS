package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.fields.util.PosPackingUtil;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.formicapi.fondation.math.operators.CSRMatrix;
import com.rae.formicapi.fondation.math.operators.DynamicCSRMatrix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Matrix-based temperature ticker using CSR sparse matrices.
 * Pre-computes the update matrix A and source vector b, then performs:
 * T^(n+1) = A·T^n + b
 */
public final class MatrixTemperatureTicker {
    public static int TICK_PERIOD = 1;
    public static float DT = TICK_PERIOD / 20f;
    public static float CAPACITY = 3e4f;

    // Cache the compiled matrices per section
    private static final Map<Long, SectionMatrixData> matrixCache = new HashMap<>();// you can't do that because it's called in sevral different server consecutively

    public static void tick(@NotNull Set<Long> tickingSections, @NotNull PhysicsWorldData data) {
        data.resetTicked();
        updateDynamicData(data);

        // --- BUILD/UPDATE MATRICES FOR DIRTY SECTIONS ---
        for (long packedSection : tickingSections) {
            SectionMatrixData sectionData = matrixCache.get(packedSection);

            // Rebuild if section is dirty or not cached
            if (sectionData == null || data.isDirty(packedSection)) {
                sectionData = buildSectionMatrix(packedSection, data);
                matrixCache.put(packedSection, sectionData);
            }
        }

        // --- PERFORM MATRIX UPDATE ---
        for (long packedSection : tickingSections) {
            data.addToTicked(packedSection);

            SectionMatrixData sectionData = matrixCache.get(packedSection);
            if (sectionData == null) continue;

            TemperatureDataLayer tempLayer = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            if (tempLayer == null) continue;

            // Extract current temperatures into vector
            double[] T_current = sectionData.T_current;
            extractTemperatures(tempLayer, T_current);

            // Perform matrix-vector multiply: T_next = A·T_current + b
            sectionData.matrix.multiply(T_current, sectionData.T_next);

            // Add source vector
            for (int i = 0; i < sectionData.T_next.length; i++) {
                sectionData.T_next[i] += sectionData.b[i];
            }

            // Clamp and write back
            boolean anyChange = writeBackTemperatures(tempLayer, sectionData.T_next, data, packedSection);

            if (anyChange) {
                data.setDirty(packedSection);
            } else {
                data.setClean(packedSection);
            }
        }

        data.setDirty();
    }

    /**
     * Build the sparse update matrix A and source vector b for a section.
     * Handles cross-section boundaries by reading neighbor data.
     */
    private static SectionMatrixData buildSectionMatrix(long packedSection, PhysicsWorldData data) {
        SectionPos sectionPos = SectionPos.of(packedSection);

        // Load data layers
        TemperatureDataLayer tempLayer = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
        TemperatureDataLayer defaultTempLayer = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
        ConductionDataLayer condLayer = data.getLayer(packedSection, DataLayerType.CONDUCTION);
        ResilienceDataLayer resLayer = data.getLayer(packedSection, DataLayerType.RESILIENCE);

        if (tempLayer == null || defaultTempLayer == null || condLayer == null || resLayer == null) {
            // Return identity matrix if data missing
            return createIdentitySection();
        }

        int              size          = 16 * 16 * 16; // Section size
        DynamicCSRMatrix matrixBuilder = new DynamicCSRMatrix(size, size);
        double[]         b             = new double[size];

        // Build matrix row by row
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int idx = index3DTo1D(x, y, z);

                    float selfCond = condLayer.get(x, y, z);
                    float selfCapacity = CAPACITY;
                    float res = resLayer.get(x, y, z);
                    float defaultTemp = defaultTempLayer.get(x, y, z);

                    if (selfCapacity <= 0) {
                        // Degenerate case - identity
                        matrixBuilder.set(idx, idx, 1.0);
                        continue;
                    }

                    // Precompute factors
                    double gamma = DT / (selfCapacity * 1.0); // Assuming unit grid spacing h=1
                    double beta = 1000.0 * DT / selfCapacity;

                    // Diagonal starts at 1
                    double diagCoeff = 1.0;

                    // Add resilience damping to diagonal
                    diagCoeff -= res * beta;

                    // Source term from resilience
                    b[idx] = res * beta * defaultTemp;

                    // Process 6 neighbors
                    int[][] neighbors = {
                            {x+1, y, z},  // +x
                            {x-1, y, z},  // -x
                            {x, y+1, z},  // +y
                            {x, y-1, z},  // -y
                            {x, y, z+1},  // +z
                            {x, y, z-1}   // -z
                    };

                    for (int[] neighbor : neighbors) {
                        int nx = neighbor[0];
                        int ny = neighbor[1];
                        int nz = neighbor[2];

                        float neighborCond;
                        int neighborIdx;

                        // Check if neighbor is in same section
                        if (nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && nz >= 0 && nz < 16) {
                            // Same section
                            neighborCond = condLayer.get(nx, ny, nz);
                            neighborIdx = index3DTo1D(nx, ny, nz);
                        } else {
                            // Cross-section boundary - read from neighbor section
                            int worldX = sectionPos.minBlockX() + nx;
                            int worldY = sectionPos.minBlockY() + ny;
                            int worldZ = sectionPos.minBlockZ() + nz;

                            int nsx = worldX >> 4;
                            int nsy = worldY >> 4;
                            int nsz = worldZ >> 4;
                            long neighborSection = SectionPos.asLong(nsx, nsy, nsz);

                            ConductionDataLayer nCondLayer = data.getLayer(neighborSection, DataLayerType.CONDUCTION);
                            if (nCondLayer == null) {
                                continue; // Skip if neighbor section not loaded
                            }

                            int nlx = worldX & 15;
                            int nly = worldY & 15;
                            int nlz = worldZ & 15;

                            neighborCond = nCondLayer.get(nlx, nly, nlz);

                            // For cross-section neighbors, we can't add to matrix
                            // Instead, we treat this as a boundary condition
                            // Read the neighbor temperature directly in the multiply step
                            // For now, skip cross-section in matrix (handle separately)
                            continue;
                        }

                        // Compute effective conductivity (harmonic mean)
                        double k_eff = harmonicMean(selfCond, neighborCond);

                        // Conduction coefficient
                        double condCoeff = (1.0 - res) * gamma * k_eff;

                        // Off-diagonal: flux from neighbor
                        matrixBuilder.add(idx, neighborIdx, condCoeff);

                        // Diagonal: flux to neighbor (negative)
                        diagCoeff -= condCoeff;
                    }

                    // Set diagonal
                    matrixBuilder.set(idx, idx, diagCoeff);
                }
            }
        }

        CSRMatrix compiled = matrixBuilder.toCSR();

        return new SectionMatrixData(
                compiled,
                b,
                new double[size],  // T_current
                new double[size]   // T_next
        );
    }

    /**
     * Improved version that handles cross-section boundaries properly
     */
    private static SectionMatrixData buildSectionMatrixWithBoundaries(
            long packedSection, PhysicsWorldData data) {

        SectionPos sectionPos = SectionPos.of(packedSection);

        // Load data layers for this section
        TemperatureDataLayer tempLayer = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
        TemperatureDataLayer defaultTempLayer = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
        ConductionDataLayer condLayer = data.getLayer(packedSection, DataLayerType.CONDUCTION);
        ResilienceDataLayer resLayer = data.getLayer(packedSection, DataLayerType.RESILIENCE);

        if (tempLayer == null || defaultTempLayer == null || condLayer == null || resLayer == null) {
            return createIdentitySection();
        }

        // Preload all neighbor sections
        Map<Long, SectionLayers> neighborSections = new HashMap<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    long nSection = SectionPos.asLong(
                            sectionPos.getX() + dx,
                            sectionPos.getY() + dy,
                            sectionPos.getZ() + dz
                    );

                    TemperatureDataLayer nTemp = data.getLayer(nSection, DataLayerType.TEMPERATURE);
                    ConductionDataLayer nCond = data.getLayer(nSection, DataLayerType.CONDUCTION);

                    if (nTemp != null && nCond != null) {
                        neighborSections.put(nSection, new SectionLayers(nTemp, nCond));
                    }
                }
            }
        }

        int size = 16 * 16 * 16;
        DynamicCSRMatrix matrixBuilder = new DynamicCSRMatrix(size, size);
        double[] b = new double[size];
        double[] boundaryFlux = new double[size]; // Store cross-section boundary contributions

        // Build matrix
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int idx = index3DTo1D(x, y, z);

                    float selfCond = condLayer.get(x, y, z);
                    float res = resLayer.get(x, y, z);
                    float defaultTemp = defaultTempLayer.get(x, y, z);

                    double gamma = DT / CAPACITY;
                    double beta = 1000.0 * DT / CAPACITY;

                    double diagCoeff = 1.0 - res * beta;
                    b[idx] = res * beta * defaultTemp;

                    // Process neighbors
                    int[][] neighbors = {
                            {x+1, y, z}, {x-1, y, z},
                            {x, y+1, z}, {x, y-1, z},
                            {x, y, z+1}, {x, y, z-1}
                    };

                    for (int[] neighbor : neighbors) {
                        int nx = neighbor[0];
                        int ny = neighbor[1];
                        int nz = neighbor[2];

                        boolean inBounds = (nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && nz >= 0 && nz < 16);

                        float neighborCond;
                        float neighborTemp = 0;

                        if (inBounds) {
                            // Same section
                            neighborCond = condLayer.get(nx, ny, nz);
                            int neighborIdx = index3DTo1D(nx, ny, nz);

                            double k_eff = harmonicMean(selfCond, neighborCond);
                            double condCoeff = (1.0 - res) * gamma * k_eff;

                            matrixBuilder.add(idx, neighborIdx, condCoeff);
                            diagCoeff -= condCoeff;
                        } else {
                            // Cross-section boundary
                            int worldX = sectionPos.minBlockX() + nx;
                            int worldY = sectionPos.minBlockY() + ny;
                            int worldZ = sectionPos.minBlockZ() + nz;

                            long nSection = SectionPos.asLong(worldX >> 4, worldY >> 4, worldZ >> 4);
                            SectionLayers layers = neighborSections.get(nSection);

                            if (layers != null) {
                                int nlx = worldX & 15;
                                int nly = worldY & 15;
                                int nlz = worldZ & 15;

                                neighborCond = layers.conduction.get(nlx, nly, nlz);
                                neighborTemp = layers.temperature.get(nlx, nly, nlz);

                                double k_eff = harmonicMean(selfCond, neighborCond);
                                double condCoeff = (1.0 - res) * gamma * k_eff;

                                // Add boundary flux to source vector
                                // This is a simplification - boundary temps are from previous timestep
                                boundaryFlux[idx] += condCoeff * neighborTemp;
                            }
                        }
                    }

                    matrixBuilder.set(idx, idx, diagCoeff);
                }
            }
        }

        // Add boundary flux to source vector
        for (int i = 0; i < size; i++) {
            b[i] += boundaryFlux[i];
        }

        return new SectionMatrixData(
                matrixBuilder.toCSR(),
                b,
                new double[size],
                new double[size]
        );
    }

    private static double harmonicMean(float a, float b) {
        if (a <= 0 || b <= 0) return 0.0;
        return 2.0 * a * b / (a + b);
    }

    private static int index3DTo1D(int x, int y, int z) {
        return x + y * 16 + z * 16 * 16;
    }

    private static void extractTemperatures(TemperatureDataLayer layer, double[] out) {
        int idx = 0;
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    out[idx++] = layer.get(x, y, z);
                }
            }
        }
    }

    private static boolean writeBackTemperatures(
            TemperatureDataLayer layer, double[] temps,
            PhysicsWorldData data, long packedSection) {

        boolean anyChange = false;
        int idx = 0;

        int sx = PosPackingUtil.unpackSectionX(packedSection);
        int sy = PosPackingUtil.unpackSectionY(packedSection);
        int sz = PosPackingUtil.unpackSectionZ(packedSection);

        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    float newTemp = (float) Math.max(
                            TemperatureDataLayer.MIN_TEMPERATURE,
                            Math.min(temps[idx++], TemperatureDataLayer.MAX_TEMPERATURE)
                    );

                    float oldTemp = layer.get(x, y, z);

                    if (Math.abs(newTemp - oldTemp) > 1e-3f) {
                        layer.set(x, y, z, newTemp);
                        anyChange = true;

                        // Handle dynamic entities
                        //TODO no for god sake
                        long pos = PosPackingUtil.packBlockPos(sx*16 + x, sy * 16 + y,sz*16+ z);

                        if (data.dynamicContains(pos)) {
                            IHaveTemperature be = data.getDynamic(pos);
                            be.addTemperature(newTemp - oldTemp);
                        }
                    }
                }
            }
        }

        return anyChange;
    }

    private static SectionMatrixData createIdentitySection() {
        int size = 16 * 16 * 16;
        DynamicCSRMatrix identity = new DynamicCSRMatrix(size, size);
        for (int i = 0; i < size; i++) {
            identity.set(i, i, 1.0);
        }
        return new SectionMatrixData(
                identity.toCSR(),
                new double[size],
                new double[size],
                new double[size]
        );
    }

    private static void updateDynamicData(@NotNull PhysicsWorldData data) {
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

            // Invalidate matrix cache for this section
            matrixCache.remove(packedSection);
        });
    }

    /**
     * Clear matrix cache for modified sections
     */
    public static void invalidateSection(long packedSection) {
        matrixCache.remove(packedSection);
    }

    /**
     * Clear entire matrix cache
     */
    public static void clearCache() {
        matrixCache.clear();
    }

    /**
     * Stores pre-compiled matrix data for a section
     */
    private static class SectionMatrixData {
        final CSRMatrix matrix;      // Update matrix A
        final double[] b;             // Source vector
        final double[] T_current;     // Work buffer for current temps
        final double[] T_next;        // Work buffer for next temps

        SectionMatrixData(CSRMatrix matrix, double[] b, double[] T_current, double[] T_next) {
            this.matrix = matrix;
            this.b = b;
            this.T_current = T_current;
            this.T_next = T_next;
        }
    }

    private static class SectionLayers {
        final TemperatureDataLayer temperature;
        final ConductionDataLayer conduction;

        SectionLayers(TemperatureDataLayer temperature, ConductionDataLayer conduction) {
            this.temperature = temperature;
            this.conduction = conduction;
        }
    }
}
