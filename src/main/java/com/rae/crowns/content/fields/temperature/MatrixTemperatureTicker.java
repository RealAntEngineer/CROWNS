package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.AbstractMatrixTicker;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.fields.util.PosPackingUtil;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.formicapi.fondation.math.solvers.LeastSquare;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
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
public final class MatrixTemperatureTicker extends AbstractMatrixTicker {

    public static        float   DT          = 1 / 20f;
    public static        float   CAPACITY    = 3e4f;

    public static void tick(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        data.resetTicked();
        updateDynamicData(data);

        PhysicsMatrix thermalMatrix = getOrBuildMatrix(tickingSections, data);
        if (thermalMatrix == null || thermalMatrix.size() == 0) {
            return;
        }

        extractAllTemperatures(thermalMatrix, data);

        double[] rhs = Arrays.copyOf(thermalMatrix.X_current(), thermalMatrix.size());;

        // add source term (DO NOT rebuild boundary or diffusion here)
        for (int i = 0; i < thermalMatrix.size(); i++) {
            rhs[i] += thermalMatrix.hrs()[i];
        }

        double[] solution = LeastSquare.solve(
                thermalMatrix.A(),
                rhs,
                200,
                1e-1f
        );

        System.arraycopy(solution, 0, thermalMatrix.X_next(), 0, solution.length);

        writeBackAllTemperatures(thermalMatrix, data);
        data.setDirty();
    }

    /**
     * Get cached matrix or incrementally update if sections changed
     */
    private static PhysicsMatrix getOrBuildMatrix(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        PhysicsMatrix cached = data.getCachedMatrix();

        if (cached == null) {
            // No cache - build from scratch
            cached = buildUnifiedMatrix(tickingSections, data);
            data.setCachedMatrix(cached);
            return cached;
        }

        // Check what changed
        LongSet cachedSections = cached.sections();
        LongSet addedSections  = new LongOpenHashSet(tickingSections);
        addedSections.removeAll(cachedSections);

        LongSet removedSections = new LongOpenHashSet(cachedSections);
        removedSections.removeAll(tickingSections);

        // If sections are exactly the same, check for dirty sections and update them
        if (addedSections.isEmpty() && removedSections.isEmpty()) {
            // Update dirty sections in-place
            for (long section : tickingSections) {
                if (data.isDirty(section)) {
                    updateSectionInMatrix(section, cached, data);
                }
            }
            return cached;
        } else if (removedSections.isEmpty()) {// Sections changed - incrementally update
            // Only additions - expand the matrix
            return expandMatrix(cached, addedSections, data);
        } else if (addedSections.isEmpty()) {
            // Only removals - shrink the matrix
            return shrinkMatrix(cached, removedSections, tickingSections);
        }
        else {
            // Both additions and removals - full rebuild is simpler
            return buildUnifiedMatrix(tickingSections, data);
        }
    }

    /**
     * Update all voxels in a section within the existing matrix
     */
    private static void updateSectionInMatrix(long packedSection, PhysicsMatrix matrix, PhysicsWorldData data) {
        int sectionStartIdx = matrix.sectionToIndex().get(packedSection);
        if (sectionStartIdx < 0) return;

        SectionPos sectionPos = SectionPos.of(packedSection);

        // Load layers
        TemperatureDataLayer defaultTempLayer = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
        ConductionDataLayer  condLayer        = data.getLayer(packedSection, DataLayerType.CONDUCTION);
        ResilienceDataLayer  resLayer         = data.getLayer(packedSection, DataLayerType.RESILIENCE);

        if (defaultTempLayer == null || condLayer == null || resLayer == null) {
            return;
        }

        // Pre-load neighbor cache
        NeighborCache neighbors = new NeighborCache(sectionPos, data, matrix.sectionToIndex());

        // Update all voxels in this section
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int localIdx  = index3DTo1D(x, y, z);
                    int globalIdx = sectionStartIdx + localIdx;

                    float selfCond    = condLayer.get(x, y, z);
                    float res         = resLayer.get(x, y, z);
                    float defaultTemp = defaultTempLayer.get(x, y, z);

                    double gamma = DT / CAPACITY;
                    double beta  = 1000.0 * DT / CAPACITY;

                    // Update source vector
                    matrix.hrs()[globalIdx] = res * beta * defaultTemp;

                    // Build new row
                    Int2DoubleMap newRow = new Int2DoubleOpenHashMap();
                    newRow.defaultReturnValue(0.0);

                    double diagCoeff = 1.0 - res * beta;

                    // Process neighbors
                    processNeighborsForRow(
                            x, y, z,
                            sectionPos, packedSection, sectionStartIdx,
                            selfCond, res, gamma,
                            globalIdx,
                            neighbors,
                            newRow,
                            matrix.hrs(),
                            diagCoeff
                    );

                    // Update the matrix row
                    matrix.A().updateRow(globalIdx, newRow);
                }
            }
        }
    }

    /**
     * Process neighbors and build row entries (helper for updateSectionInMatrix)
     */
    private static void processNeighborsForRow(
            int x, int y, int z,
            SectionPos sectionPos, long packedSection, int sectionStartIdx,
            float selfCond, float res, double gamma,
            int globalIdx,
            NeighborCache neighbors,
            Int2DoubleMap row,
            double[] b,
            double diagCoeff
    ) {

        for (int[] offset : OFFSETS) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            int nz = z + offset[2];

            NeighborInfo neighbor = getNeighborInfo(nx, ny, nz, sectionPos, packedSection, sectionStartIdx, neighbors);

            if (neighbor == null) continue;

            float  b1        = neighbor.conductivity();
            double k_eff     = (selfCond <= 0 || b1 <= 0) ? 0.0 : 2.0 * selfCond * b1 / (selfCond + b1);
            double condCoeff = (1.0 - res) * gamma * k_eff;

            if (neighbor.isInMatrix()) {
                row.put(neighbor.globalIndex(), condCoeff);
                diagCoeff -= condCoeff;
            } else {
                // boundary term only affects RHS, NOT diagonal
                b[globalIdx] += condCoeff * neighbor.temperature();
                // DO NOT add to diagCoeff here!
            }
        }

        row.put(globalIdx, diagCoeff);
    }

    /**
     * Expand matrix to include new sections
     */
    private static PhysicsMatrix expandMatrix(PhysicsMatrix existing, LongSet addedSections, PhysicsWorldData data) {
        // Calculate new total size
        int addedSize    = addedSections.size() * 4096;
        int newTotalSize = existing.size() + addedSize;

        // Build new section index map
        Long2IntMap newSectionToIndex = new Long2IntOpenHashMap(existing.sectionToIndex());

        int      nextIndex   = existing.size();
        LongList sortedAdded = new LongArrayList(addedSections);
        sortedAdded.sort(null);

        for (long section : sortedAdded) {
            newSectionToIndex.put(section, nextIndex);
            nextIndex += 4096;
        }

        // Create expanded matrix
        MutableCSRMatrix newMatrix = new MutableCSRMatrix(newTotalSize, newTotalSize);
        double[]         newB      = new double[newTotalSize];

        // Copy existing matrix rows
        for (int i = 0; i < existing.size(); i++) {
            Int2DoubleMap row = existing.A().getRowCopy(i);
            newMatrix.updateRow(i, row);
            newB[i] = existing.hrs()[i];
        }

        // Build new section contributions
        for (long packedSection : sortedAdded) {
            int sectionStartIdx = newSectionToIndex.get(packedSection);
            buildSectionContribution(packedSection, sectionStartIdx, newSectionToIndex, newMatrix, newB, data);
        }

        // Update cross-section boundaries for existing sections that now neighbor new sections
        updateCrossSectionBoundaries(existing.sections(), addedSections, newSectionToIndex, newMatrix, newB, data);

        // Create new thermal matrix
        LongSet allSections = new LongOpenHashSet(existing.sections());
        allSections.addAll(addedSections);

        return new PhysicsMatrix(
                allSections,
                newSectionToIndex,
                newMatrix,
                newB,
                new double[newTotalSize],
                new double[newTotalSize],
                newTotalSize
        );
    }

    /**
     * Shrink matrix to remove sections
     */
    private static PhysicsMatrix shrinkMatrix(PhysicsMatrix existing, LongSet removedSections, LongSet remainingSections) {
        if (remainingSections.isEmpty()) {
            return new PhysicsMatrix(
                    LongSets.EMPTY_SET,
                    new Long2IntOpenHashMap(),
                    new MutableCSRMatrix(0, 0),
                    new double[0],
                    new double[0],
                    new double[0],
                    0
            );
        }

        // Build new index mapping (compact)
        Long2IntMap newSectionToIndex = new Long2IntOpenHashMap();
        newSectionToIndex.defaultReturnValue(-1);

        LongList sortedRemaining = new LongArrayList(remainingSections);
        sortedRemaining.sort(null);

        int newIndex = 0;
        for (long section : sortedRemaining) {
            newSectionToIndex.put(section, newIndex);
            newIndex += 4096;
        }

        int newSize = newIndex;

        // Create compacted matrix
        MutableCSRMatrix newMatrix = new MutableCSRMatrix(newSize, newSize);
        double[]         newB      = new double[newSize];

        // Copy and remap rows
        for (long section : sortedRemaining) {
            int oldStartIdx = existing.sectionToIndex().get(section);
            int newStartIdx = newSectionToIndex.get(section);

            for (int localIdx = 0; localIdx < 4096; localIdx++) {
                int oldGlobalIdx = oldStartIdx + localIdx;
                int newGlobalIdx = newStartIdx + localIdx;

                // Get old row and remap indices
                Int2DoubleMap oldRow = existing.A().getRowCopy(oldGlobalIdx);
                Int2DoubleMap newRow = new Int2DoubleOpenHashMap();

                for (Int2DoubleMap.Entry entry : oldRow.int2DoubleEntrySet()) {
                    int    oldColIdx = entry.getIntKey();
                    double value     = entry.getDoubleValue();

                    // Find which section this column belongs to
                    long colSection = findSectionForIndex(oldColIdx, existing.sectionToIndex());

                    if (colSection != -1 && remainingSections.contains(colSection)) {
                        // Remap to new index
                        int colLocalIdx = oldColIdx - existing.sectionToIndex().get(colSection);
                        int newColIdx   = newSectionToIndex.get(colSection) + colLocalIdx;
                        newRow.put(newColIdx, value);
                    }
                }

                newMatrix.updateRow(newGlobalIdx, newRow);
                newB[newGlobalIdx] = existing.hrs()[oldGlobalIdx];
            }
        }

        return new PhysicsMatrix(
                new LongOpenHashSet(remainingSections),
                newSectionToIndex,
                newMatrix,
                newB,
                new double[newSize],
                new double[newSize],
                newSize
        );
    }

    /**
     * Update boundary voxels when neighboring sections are added
     */
    private static void updateCrossSectionBoundaries(
            LongSet existingSections,
            LongSet addedSections,
            Long2IntMap sectionToIndex,
            MutableCSRMatrix matrix,
            double[] b,
            PhysicsWorldData data
    ) {
        // For each existing section, check if any added sections are neighbors
        for (long existingSection : existingSections) {
            SectionPos existingPos = SectionPos.of(existingSection);

            boolean hasNewNeighbor = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        long neighborSection = SectionPos.asLong(
                                existingPos.getX() + dx,
                                existingPos.getY() + dy,
                                existingPos.getZ() + dz
                        );

                        if (addedSections.contains(neighborSection)) {
                            hasNewNeighbor = true;
                            break;
                        }
                    }
                    if (hasNewNeighbor) break;
                }
                if (hasNewNeighbor) break;
            }

            if (hasNewNeighbor) {
                // Update boundary voxels of this section
                int sectionStartIdx = sectionToIndex.get(existingSection);

                ConductionDataLayer condLayer = data.getLayer(existingSection, DataLayerType.CONDUCTION);
                ResilienceDataLayer resLayer  = data.getLayer(existingSection, DataLayerType.RESILIENCE);

                if (condLayer == null || resLayer == null) continue;

                NeighborCache neighbors = new NeighborCache(existingPos, data, sectionToIndex);

                // Only update voxels on the boundary (faces of the 16x16x16 cube)
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        for (int x = 0; x < 16; x++) {
                            // Skip interior voxels
                            if (x > 0 && x < 15 && y > 0 && y < 15 && z > 0 && z < 15) {
                                continue;
                            }

                            int localIdx  = index3DTo1D(x, y, z);
                            int globalIdx = sectionStartIdx + localIdx;

                            // Check if any neighbors cross into newly added sections
                            boolean needsUpdate = false;
                            for (int[] offset : OFFSETS) {
                                int nx = x + offset[0];
                                int ny = y + offset[1];
                                int nz = z + offset[2];

                                if (nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && nz >= 0 && nz < 16) {
                                    continue; // Same section
                                }

                                int worldX = existingPos.minBlockX() + nx;
                                int worldY = existingPos.minBlockY() + ny;
                                int worldZ = existingPos.minBlockZ() + nz;

                                long nSection = SectionPos.asLong(worldX >> 4, worldY >> 4, worldZ >> 4);

                                if (addedSections.contains(nSection)) {
                                    needsUpdate = true;
                                    break;
                                }
                            }

                            if (needsUpdate) {
                                updateVoxelRow(x, y, z, existingSection, existingPos, sectionStartIdx, matrix, b, data, neighbors);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper to find which section an index belongs to
     */
    private static long findSectionForIndex(int index, Long2IntMap sectionToIndex) {
        for (Long2IntMap.Entry entry : sectionToIndex.long2IntEntrySet()) {
            int  startIdx = entry.getIntValue();
            long section  = entry.getLongKey();

            if (index >= startIdx && index < startIdx + 4096) {
                return section;
            }
        }
        return -1;
    }

    /**
     * Build the unified matrix for all ticking sections at once
     */
    private static PhysicsMatrix buildUnifiedMatrix(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        if (tickingSections.isEmpty()) {
            return new PhysicsMatrix(
                    LongSets.EMPTY_SET,
                    new Long2IntOpenHashMap(),
                    new MutableCSRMatrix(0, 0),
                    new double[0],
                    new double[0],
                    new double[0],
                    0
            );
        }

        // Sort sections for consistent indexing
        LongList sortedSections = new LongArrayList(tickingSections);
        sortedSections.sort(null);

        int totalNodes = sortedSections.size() * 4096; // 16x16x16 voxels per section

        // Build section → start index map
        Long2IntMap sectionToIndex = new Long2IntOpenHashMap();
        sectionToIndex.defaultReturnValue(-1);

        int currentIndex = 0;
        for (long section : sortedSections) {
            sectionToIndex.put(section, currentIndex);
            currentIndex += 4096;
        }

        // Create data structures
        MutableCSRMatrix matrix = new MutableCSRMatrix(totalNodes, totalNodes);
        double[]         b      = new double[totalNodes];

        // Build matrix contributions for each section
        for (long packedSection : sortedSections) {
            int sectionStartIdx = sectionToIndex.get(packedSection);
            buildSectionContribution(packedSection, sectionStartIdx, sectionToIndex, matrix, b, data);
        }

        return new PhysicsMatrix(
                new LongOpenHashSet(tickingSections),
                sectionToIndex,
                matrix,
                b,
                new double[totalNodes],
                new double[totalNodes],
                totalNodes
        );
    }

    /**
     * Build the matrix contribution for a single section
     */
    private static void buildSectionContribution(
            long packedSection,
            int sectionStartIdx,
            Long2IntMap sectionToIndex,
            MutableCSRMatrix matrix,
            double[] b,
            PhysicsWorldData data
    ) {
        SectionPos sectionPos = SectionPos.of(packedSection);
        // Preload neighbor data
        NeighborCache neighbors = new NeighborCache(sectionPos, data, sectionToIndex);

        // Build voxel rows
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    updateVoxelRow(x, y, z, packedSection, sectionPos, sectionStartIdx, matrix, b, data, neighbors);

                }
            }
        }
    }

    /**
     * Update a single voxel row in the matrix
     */
    private static boolean updateVoxelRow(
            int x, int y, int z,
            long packedSection,
            SectionPos sectionPos,
            int sectionStartIdx,
            MutableCSRMatrix matrix,
            double[] b,
            PhysicsWorldData data,
            NeighborCache neighbors
    ) {
        TemperatureDataLayer defaultTempLayer = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
        ConductionDataLayer  condLayer        = data.getLayer(packedSection, DataLayerType.CONDUCTION);
        ResilienceDataLayer  resLayer         = data.getLayer(packedSection, DataLayerType.RESILIENCE);

        if (defaultTempLayer == null || condLayer == null || resLayer == null) {
            return false;
        }

        int localIdx  = index3DTo1D(x, y, z);
        int globalIdx = sectionStartIdx + localIdx;

        float selfCond    = condLayer.get(x, y, z);
        float res         = resLayer.get(x, y, z);
        float defaultTemp = defaultTempLayer.get(x, y, z);

        double gamma = DT / CAPACITY;
        double beta  = 1000.0 * gamma;

        // Update source
        b[globalIdx] = res * beta * defaultTemp;

        // Build new row
        Int2DoubleMap newRow = new Int2DoubleOpenHashMap();
        newRow.defaultReturnValue(0.0);

        double diag = 1.0 + res * beta;

        for (int[] offset : OFFSETS) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            int nz = z + offset[2];

            NeighborInfo neighbor = getNeighborInfo(nx, ny, nz, sectionPos, packedSection, sectionStartIdx, neighbors);

            if (neighbor == null) continue;

            float  neighborCond = neighbor.conductivity();
            double k_eff        = (selfCond <= 0 || neighborCond <= 0)
                    ? 0.0
                    : 2.0 * selfCond * neighborCond / (selfCond + neighborCond);

            double condCoeff = (1.0 - res) * gamma * k_eff;

            if (neighbor.isInMatrix()) {
                newRow.put(neighbor.globalIndex(), -condCoeff);
                diag += condCoeff;
            } else {

                b[globalIdx] += condCoeff * neighbor.temperature();
                diag += condCoeff;
            }
        }

        newRow.put(globalIdx, diag);

        matrix.updateRow(globalIdx, newRow);

        return true;
    }

    /**
     * Extract all temperatures from sections into the global vector
     */
    private static void extractAllTemperatures(PhysicsMatrix matrix, PhysicsWorldData data) {
        for (Long2IntMap.Entry entry : matrix.sectionToIndex().long2IntEntrySet()) {
            long section  = entry.getLongKey();
            int  startIdx = entry.getIntValue();

            TemperatureDataLayer layer = data.getLayer(section, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            int idx = startIdx;
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        matrix.X_current()[idx++] = layer.get(x, y, z);
                    }
                }
            }

            data.addToTicked(section);
        }
    }

    /**
     * Write back all temperatures from global vector to sections
     */
    private static void writeBackAllTemperatures(PhysicsMatrix matrix, PhysicsWorldData data) {
        for (Long2IntMap.Entry entry : matrix.sectionToIndex().long2IntEntrySet()) {
            long section  = entry.getLongKey();
            int  startIdx = entry.getIntValue();

            TemperatureDataLayer layer = data.getLayer(section, DataLayerType.TEMPERATURE);
            if (layer == null) continue;

            boolean anyChange = false;
            int     sx        = PosPackingUtil.unpackSectionX(section);
            int     sy        = PosPackingUtil.unpackSectionY(section);
            int     sz        = PosPackingUtil.unpackSectionZ(section);

            int idx = startIdx;
            int x, y, z = 0;
            for (z = 0; z < 16; z++) {
                for (y = 0; y < 16; y++) {
                    for (x = 0; x < 16; x++) {
                        float newTemp = (float) Math.clamp(matrix.X_next()[idx++],
                                TemperatureDataLayer.MIN_TEMPERATURE, TemperatureDataLayer.MAX_TEMPERATURE);

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

    /**
     * Stamp multiple voxels at once (more efficient than individual stamps).
     * Also updates neighbor rows that reference the stamped voxels.
     */
    public static void stampVoxels(@NotNull Set<BlockPos> positions, @NotNull PhysicsWorldData data) {
        PhysicsMatrix matrix = data.getCachedMatrix();

        // Group by section for efficiency
        Map<Long, List<BlockPos>> bySection = new HashMap<>();
        for (BlockPos pos : positions) {
            long section = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            bySection.computeIfAbsent(section, k -> new ArrayList<>()).add(pos);
        }

        Set<BlockPos> needNeighborUpdate = new HashSet<>();

        // Stamp all primary voxels
        for (Map.Entry<Long, List<BlockPos>> entry : bySection.entrySet()) {
            long section = entry.getKey();

            if (!matrix.sections().contains(section)) {
                continue;
            }

            for (BlockPos pos : entry.getValue()) {
                if (stampVoxel(pos, data)) {
                    // Mark neighbors for update (but don't duplicate the stamped position itself)
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (dx == 0 && dy == 0 && dz == 0) continue;
                                BlockPos neighborPos = pos.offset(dx, dy, dz);//move this to long instead of pos
                                // Only add if it's not in the positions list (avoid duplication)
                                if (!positions.contains(neighborPos)) {
                                    needNeighborUpdate.add(neighborPos);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Update neighbor rows (they reference the stamped voxels)
        // No need to check if already stamped since we filtered them out above
        for (BlockPos neighborPos : needNeighborUpdate) {
            stampVoxel(neighborPos, data);
        }

    }

    /**
     * Stamp a single voxel's properties into the existing matrix.
     * Updates the matrix row for this voxel and its neighbors without rebuilding everything.
     *
     * @param pos  Block position to update
     * @param data Physics world data
     * @return true if stamp was successful, false if matrix needs full rebuild
     */
    public static boolean stampVoxel(@NotNull BlockPos pos, @NotNull PhysicsWorldData data) {
        PhysicsMatrix matrix = data.getCachedMatrix();

        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;
        long packedSection = SectionPos.asLong(sx, sy, sz);

        int sectionStartIdx = matrix.sectionToIndex().get(packedSection);
        if (sectionStartIdx < 0) {
            return false; // Section not in matrix, needs rebuild
        }

        SectionPos sectionPos = SectionPos.of(packedSection);

        NeighborCache neighbors = new NeighborCache(sectionPos, data, matrix.sectionToIndex());

        return updateVoxelRow(pos.getX(), pos.getY(), pos.getZ(),
                packedSection, sectionPos, sectionStartIdx, matrix.A(), matrix.hrs(), data, neighbors);
    }

    private static int index3DTo1D(int x, int y, int z) {
        return x + y * 16 + z * 16 * 16;
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
            ConductionDataLayer  condLayer = neighbors.getSectionConduction(6);
            TemperatureDataLayer tempLayer = neighbors.getSectionTemperature(6);

            if (condLayer == null || tempLayer == null) return null;

            int localIdx  = index3DTo1D(nx, ny, nz);
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
        int nidx =
                nx == -1 ? 0 :
                nx == 16 ? 1 :
                ny == -1 ? 2 :
                ny == 16 ? 3 :
                nz == -1 ? 4 : 5;

        ConductionDataLayer  nCondLayer = neighbors.getSectionConduction(nidx);
        TemperatureDataLayer nTempLayer = neighbors.getSectionTemperature(nidx);

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
            int neighborLocalIdx  = index3DTo1D(nlx, nly, nlz);
            int neighborGlobalIdx = neighborSectionStart + neighborLocalIdx;

            return new NeighborInfo(neighborCond, neighborTemp, neighborGlobalIdx, true);
        } else {
            // Neighbor section not in ticking set - boundary condition
            return new NeighborInfo(neighborCond, neighborTemp, -1, false);
        }
    }

    private static void updateDynamicData(@NotNull PhysicsWorldData data) {
        Set<BlockPos> toStamp = new HashSet<>();

        data.getDynamicData().forEach((key, value) -> {
            BlockPos pos = BlockPos.of(key);

            if (value instanceof BlockEntity blockEntity && blockEntity.isRemoved()) {
                return;
            }

            int  sx            = pos.getX() >> 4;
            int  sy            = pos.getY() >> 4;
            int  sz            = pos.getZ() >> 4;
            long packedSection = SectionPos.asLong(sx, sy, sz);

            TemperatureDataLayer temperatureData        = data.getLayer(packedSection, DataLayerType.TEMPERATURE);
            TemperatureDataLayer defaultTemperatureData = data.getLayer(packedSection, DataLayerType.DEFAULT_TEMPERATURE);
            ConductionDataLayer  conductionData         = data.getLayer(packedSection, DataLayerType.CONDUCTION);
            ResilienceDataLayer  resilienceData         = data.getLayer(packedSection, DataLayerType.RESILIENCE);

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

    // === RECORDS AND HELPER CLASSES ===

}
