package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.fields.util.PosPackingUtil;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.formicapi.fondation.math.operators.CSRMatrix;
import com.rae.formicapi.fondation.math.operators.DynamicCSRMatrix;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Optimized matrix-based temperature ticker.
 * Builds a single unified CSR matrix for ALL ticking sections at once.
 * Only rebuilds when sections are added/removed/modified.
 */
public final class UnifiedMatrixTemperatureTicker {
    public static int TICK_PERIOD = 1;
    public static float DT = TICK_PERIOD / 20f;
    public static float CAPACITY = 3e4f;

    public static void tick(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        data.resetTicked();
        updateDynamicData(data);

        // Get or build unified matrix
        ThermalMatrix thermalMatrix = getOrBuildMatrix(tickingSections, data);
        if (thermalMatrix.size == 0) {
            return;
        }

        // Extract current temperatures
        extractAllTemperatures(thermalMatrix, data);

        // Perform single matrix-vector multiply: T_next = A·T_current + b
        thermalMatrix.matrix.multiply(thermalMatrix.T_current, thermalMatrix.T_next);

        // Add source vector
        for (int i = 0; i < thermalMatrix.size; i++) {
            thermalMatrix.T_next[i] += thermalMatrix.b[i];
        }

        // Write back and check for changes
        writeBackAllTemperatures(thermalMatrix, data);

        data.setDirty();
    }

    /**
     * Get cached matrix or rebuild if sections changed
     */
    private static ThermalMatrix getOrBuildMatrix(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        // Check if we have a cached matrix
        ThermalMatrix cached = data.getCachedMatrix();

        if (cached != null && cached.sectionsMatch(tickingSections)) {
            // Check if any section is dirty
            boolean anyDirty = false;
            for (long section : tickingSections) {
                if (data.isDirty(section)) {
                    anyDirty = true;
                    break;
                }
            }

            if (!anyDirty) {
                return cached; // Use cached matrix
            }
        }

        // Rebuild matrix
        ThermalMatrix newMatrix = buildUnifiedMatrix(tickingSections, data);
        data.setCachedMatrix(newMatrix);
        return newMatrix;
    }

    /**
     * Build a single unified matrix for all ticking sections.
     * Maps section indices to global matrix indices for efficient lookup.
     */
    private static ThermalMatrix buildUnifiedMatrix(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        if (tickingSections.isEmpty()) {
            return new ThermalMatrix(
                    LongSets.EMPTY_SET,
                    new Long2IntOpenHashMap(),
                    null,
                    new double[0],
                    new double[0],
                    new double[0],
                    0
            );
        }

        // Build section index map: section position -> starting index in global vector
        Long2IntMap sectionToIndex = new Long2IntOpenHashMap();
        sectionToIndex.defaultReturnValue(-1);

        LongList sortedSections = new LongArrayList(tickingSections);
        sortedSections.sort(null); // Sort for consistency

        int globalIndex = 0;
        for (long section : sortedSections) {
            sectionToIndex.put(section, globalIndex);
            globalIndex += 4096; // 16^3 nodes per section
        }

        int totalSize = globalIndex;
        DynamicCSRMatrix matrixBuilder = new DynamicCSRMatrix(totalSize, totalSize);
        double[] b = new double[totalSize];

        // Build matrix for each section
        for (long packedSection : sortedSections) {
            int sectionStartIdx = sectionToIndex.get(packedSection);
            buildSectionContribution(packedSection, sectionStartIdx, sectionToIndex, matrixBuilder, b, data);
        }

        CSRMatrix compiled = matrixBuilder.toCSR();

        return new ThermalMatrix(
                new LongOpenHashSet(tickingSections),
                sectionToIndex,
                compiled,
                b,
                new double[totalSize], // T_current
                new double[totalSize], // T_next
                totalSize
        );
    }

    /**
     * Build the matrix contribution for a single section, handling cross-section boundaries.
     */
    private static void buildSectionContribution(
            long packedSection,
            int sectionStartIdx,
            Long2IntMap sectionToIndex,
            DynamicCSRMatrix matrix,
            double[] b,
            PhysicsWorldData data
    ) {
        SectionPos sectionPos = SectionPos.of(packedSection);

        // Load layers
        TemperatureDataLayer defaultTempLayer = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
        ConductionDataLayer condLayer = data.getLayer(packedSection, DataLayerType.CONDUCTION);
        ResilienceDataLayer resLayer = data.getLayer(packedSection, DataLayerType.RESILIENCE);

        if (defaultTempLayer == null || condLayer == null || resLayer == null) {
            // Set identity for missing data
            for (int i = 0; i < 4096; i++) {
                matrix.set(sectionStartIdx + i, sectionStartIdx + i, 1.0);
            }
            return;
        }

        // Pre-load neighbor sections for boundary handling
        NeighborCache neighbors = new NeighborCache(sectionPos, data, sectionToIndex);

        // Build matrix row by row
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int localIdx = index3DTo1D(x, y, z);
                    int globalIdx = sectionStartIdx + localIdx;

                    float selfCond = condLayer.get(x, y, z);
                    float res = resLayer.get(x, y, z);
                    float defaultTemp = defaultTempLayer.get(x, y, z);

                    double gamma = DT / CAPACITY;
                    double beta = 1000.0 * DT / CAPACITY;

                    double diagCoeff = 1.0 - res * beta;
                    b[globalIdx] = res * beta * defaultTemp;

                    // Process 6 neighbors
                    processNeighbors(
                            x, y, z,
                            sectionPos, packedSection, sectionStartIdx,
                            selfCond, res, gamma,
                            globalIdx,
                            neighbors,
                            matrix,
                            b
                    );

                    // Set diagonal after accumulating neighbor contributions
                    matrix.set(globalIdx, globalIdx, diagCoeff);
                }
            }
        }
    }

    /**
     * Process all 6 neighbors for a voxel, handling both same-section and cross-section cases.
     */
    private static void processNeighbors(
            int x, int y, int z,
            SectionPos sectionPos, long packedSection, int sectionStartIdx,
            float selfCond, float res, double gamma,
            int globalIdx,
            NeighborCache neighbors,
            DynamicCSRMatrix matrix,
            double[] b
    ) {
        // 6 directions: +x, -x, +y, -y, +z, -z
        int[][] offsets = {
                {1, 0, 0}, {-1, 0, 0},
                {0, 1, 0}, {0, -1, 0},
                {0, 0, 1}, {0, 0, -1}
        };

        for (int[] offset : offsets) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            int nz = z + offset[2];

            NeighborInfo neighbor = getNeighborInfo(nx, ny, nz, sectionPos, packedSection, sectionStartIdx, neighbors);

            if (neighbor == null) continue; // Neighbor not loaded

            double k_eff = harmonicMean(selfCond, neighbor.conductivity());
            double condCoeff = (1.0 - res) * gamma * k_eff;

            if (neighbor.isInMatrix()) {
                // Neighbor is in the global matrix - add off-diagonal entry
                matrix.add(globalIdx, neighbor.globalIndex(), condCoeff);

                // Subtract from diagonal (we'll set it after the loop)
                double currentDiag = matrix.get(globalIdx, globalIdx);
                matrix.set(globalIdx, globalIdx, currentDiag - condCoeff);
            } else {
                // Neighbor is outside ticking sections - treat as boundary condition
                // Add flux contribution to source vector
                b[globalIdx] += condCoeff * neighbor.temperature();
            }
        }
    }

    /**
     * Get information about a neighbor voxel (same section or cross-section)
     */
    private static NeighborInfo getNeighborInfo(
            int nx, int ny, int nz,
            SectionPos sectionPos,
            long packedSection,
            int sectionStartIdx,
            NeighborCache neighbors
    ) {
        // Check if in same section
        if (nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && nz >= 0 && nz < 16) {
            // Same section
            ConductionDataLayer condLayer = neighbors.getSectionConduction(packedSection);
            TemperatureDataLayer tempLayer = neighbors.getSectionTemperature(packedSection);

            if (condLayer == null || tempLayer == null) return null;

            int localIdx = index3DTo1D(nx, ny, nz);
            int globalIdx = sectionStartIdx + localIdx;

            return new NeighborInfo(
                    condLayer.get(nx, ny, nz),
                    tempLayer.get(nx, ny, nz),
                    globalIdx,
                    true // In matrix
            );
        }

        // Cross-section boundary
        int worldX = sectionPos.minBlockX() + nx;
        int worldY = sectionPos.minBlockY() + ny;
        int worldZ = sectionPos.minBlockZ() + nz;

        long nSection = SectionPos.asLong(worldX >> 4, worldY >> 4, worldZ >> 4);

        ConductionDataLayer nCondLayer = neighbors.getSectionConduction(nSection);
        TemperatureDataLayer nTempLayer = neighbors.getSectionTemperature(nSection);

        if (nCondLayer == null || nTempLayer == null) return null;

        int nlx = worldX & 15;
        int nly = worldY & 15;
        int nlz = worldZ & 15;

        float neighborCond = nCondLayer.get(nlx, nly, nlz);
        float neighborTemp = nTempLayer.get(nlx, nly, nlz);

        // Check if neighbor section is in the ticking set (and thus in the matrix)
        int neighborSectionStart = neighbors.getSectionStartIndex(nSection);

        if (neighborSectionStart >= 0) {
            // Neighbor is in matrix
            int neighborLocalIdx = index3DTo1D(nlx, nly, nlz);
            int neighborGlobalIdx = neighborSectionStart + neighborLocalIdx;

            return new NeighborInfo(neighborCond, neighborTemp, neighborGlobalIdx, true);
        } else {
            // Neighbor section not in ticking set - boundary condition
            return new NeighborInfo(neighborCond, neighborTemp, -1, false);
        }
    }

    /**
     * Extract all temperatures from sections into the global vector
     */
    private static void extractAllTemperatures(ThermalMatrix matrix, PhysicsWorldData data) {
        for (Long2IntMap.Entry entry : matrix.sectionToIndex.long2IntEntrySet()) {
            long section = entry.getLongKey();
            int startIdx = entry.getIntValue();

            TemperatureDataLayer layer = data.getLayer(section, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            int idx = startIdx;
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        matrix.T_current[idx++] = layer.get(x, y, z);
                    }
                }
            }

            data.addToTicked(section);
        }
    }

    /**
     * Write back all temperatures from global vector to sections
     */
    private static void writeBackAllTemperatures(ThermalMatrix matrix, PhysicsWorldData data) {
        for (Long2IntMap.Entry entry : matrix.sectionToIndex.long2IntEntrySet()) {
            long section = entry.getLongKey();
            int startIdx = entry.getIntValue();

            TemperatureDataLayer layer = data.getLayer(section, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            boolean anyChange = false;
            int sx = PosPackingUtil.unpackSectionX(section);
            int sy = PosPackingUtil.unpackSectionY(section);
            int sz = PosPackingUtil.unpackSectionZ(section);

            int idx = startIdx;
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        float newTemp = (float) Math.max(
                                TemperatureDataLayer.MIN_TEMPERATURE,
                                Math.min(matrix.T_next[idx++], TemperatureDataLayer.MAX_TEMPERATURE)
                        );

                        float oldTemp = layer.get(x, y, z);

                        if (Math.abs(newTemp - oldTemp) > 1e-3f) {
                            layer.set(x, y, z, newTemp);
                            anyChange = true;

                            // Handle dynamic entities
                            long pos = PosPackingUtil.packBlockPos(sx * 16 + x, sy * 16 + y, sz * 16 + z);
                            if (data.dynamicContains(pos)) {
                                IHaveTemperature be = data.getDynamic(pos);
                                be.addTemperature(newTemp - oldTemp);
                            }
                        }
                    }
                }
            }

            if (anyChange) {
                data.setDirty(section);
            } else {
                data.setClean(section);
            }
        }
    }

    private static double harmonicMean(float a, float b) {
        if (a <= 0 || b <= 0) return 0.0;
        return 2.0 * a * b / (a + b);
    }

    private static int index3DTo1D(int x, int y, int z) {
        return x + y * 16 + z * 16 * 16;
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
            //data.invalidateThermalMatrix(); // Invalidate cached matrix -> rebuild it every tick what a nice idea
        });
    }

    // === RECORDS AND HELPER CLASSES ===

    /**
     * Stores the unified thermal matrix and metadata
     */
    public record ThermalMatrix(
            LongSet sections,              // Which sections this matrix covers
            Long2IntMap sectionToIndex,    // Section -> starting index in vectors
            CSRMatrix matrix,              // The compiled A matrix
            double[] b,                    // Source vector
            double[] T_current,            // Work buffer for current temps
            double[] T_next,               // Work buffer for next temps
            int size                       // Total number of nodes
    ) {
        public boolean sectionsMatch(LongSet other) {
            return sections.equals(other);
        }
    }

    /**
     * Information about a neighbor voxel
     */
    private record NeighborInfo(
            float conductivity,
            float temperature,
            int globalIndex,      // Index in global matrix (-1 if not in matrix)
            boolean isInMatrix    // True if neighbor is part of ticking sections
    ) {}

    /**
     * Cache for neighbor section data to avoid repeated lookups
     */
    private static class NeighborCache {
        private final PhysicsWorldData data;
        private final Long2IntMap sectionToIndex;
        private final Map<Long, ConductionDataLayer> condCache = new HashMap<>();
        private final Map<Long, TemperatureDataLayer> tempCache = new HashMap<>();

        public NeighborCache(SectionPos center, PhysicsWorldData data, Long2IntMap sectionToIndex) {
            this.data = data;
            this.sectionToIndex = sectionToIndex;

            // Pre-load center and 26 neighbors
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        long section = SectionPos.asLong(
                                center.getX() + dx,
                                center.getY() + dy,
                                center.getZ() + dz
                        );

                        ConductionDataLayer cond = data.getLayer(section, DataLayerType.CONDUCTION);
                        TemperatureDataLayer temp = data.getLayer(section, DataLayerType.TEMPERATURE);

                        if (cond != null) condCache.put(section, cond);
                        if (temp != null) tempCache.put(section, temp);
                    }
                }
            }
        }

        public ConductionDataLayer getSectionConduction(long section) {
            return condCache.get(section);
        }

        public TemperatureDataLayer getSectionTemperature(long section) {
            return tempCache.get(section);
        }

        public int getSectionStartIndex(long section) {
            return sectionToIndex.get(section);
        }
    }
}
