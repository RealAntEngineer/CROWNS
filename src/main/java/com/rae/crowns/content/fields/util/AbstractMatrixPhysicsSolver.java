package com.rae.crowns.content.fields.util;

import com.rae.formicapi.fondation.math.operators.HashSparseMatrix;
import com.rae.formicapi.fondation.math.operators.PaddedCSRMatrix;
import com.rae.formicapi.fondation.math.solvers.ConjugateGradient;
import com.rae.formicapi.fondation.math.solvers.LeastSquare;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.lwjgl.system.NonnullDefault;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Abstract matrix-based physics solver.
 *
 * <p>Handles all the infrastructure for building and solving sparse linear systems
 * across voxel grids. Subclasses only implement the physics: field I/O and per-voxel
 * equation coefficients.
 *
 * <p>Assembly uses {@link PaddedCSRMatrix} — a flat-array sparse matrix with a fixed
 * slot count per row and no HashMap. Rows are overwritten in-place on dirty updates via
 * {@link #buildSectionRows} and individual voxel updates via {@link #stampVoxels}.
 * The matrix is passed directly to {@link ConjugateGradient2} each tick — no CSR
 * compilation step needed.
 *
 * <p>Section lifecycle:
 * <ul>
 *   <li>No change → return cached matrix as-is</li>
 *   <li>Sections added → {@link #extendMatrix}: grow flat arrays, fill new rows,
 *       rebuild face neighbors that gained interior coupling</li>
 *   <li>Sections removed → {@link #shrinkMatrix}: swap-with-last, rewrite affected
 *       column indices, rebuild face neighbors that lost interior coupling</li>
 *   <li>Mixed add+remove → shrink then extend</li>
 * </ul>
 *
 * @param <M> concrete subclass of {@link PhysicsMatrix}
 */
@NonnullDefault
public abstract class AbstractMatrixPhysicsSolver<M extends AbstractMatrixPhysicsSolver.PhysicsMatrix> {

    protected static final byte[][]  NEIGHBOR_OFFSETS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };
    protected static final boolean[] IS_BOUNDARY      = new boolean[4096];

    static {
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    if (x == 0 || x == 15 ||
                            y == 0 || y == 15 ||
                            z == 0 || z == 15)
                        IS_BOUNDARY[index3DTo1D(x, y, z)] = true;
    }

    float dt;
    // -------------------------------------------------------------------------
    // Abstract — physics specific
    // -------------------------------------------------------------------------

    /**
     * Timestep used by this solver.
     */
    protected float getTimeStep() {
        return dt;
    }

    public void setTimeStep(float dt) {
        this.dt = dt;
    }

    /**
     * Data layer types needed for neighbor lookups (preloaded into {@link NeighborCache}).
     */
    protected abstract DataLayerType[] getRequiredLayers();

    /**
     * Allocate a new, empty physics matrix of the concrete type.
     */
    protected abstract M createMatrix(LongSet sections, Long2IntMap sectionToIndex, int totalSize);

    /**
     * Store the matrix in world data.
     */
    protected abstract void setCachedMatrix(PhysicsWorldData data, M matrix);

    /**
     * Copy the current field values from world data into {@code matrix}'s
     * field vector(s), so they are available when building the RHS.
     */
    protected abstract void extractFieldValues(M matrix, PhysicsWorldData data);

    /**
     * Write the solved field values from {@code matrix} back to world data.
     */
    protected abstract void writeBackFieldValues(M matrix, PhysicsWorldData data);

    /**
     * Update dynamic sources (e.g. block entities) before matrix assembly.
     * Implementations should call {@link #stampVoxels} for any positions they modify
     * so the dirty tracker knows which sections need row updates.
     */
    protected abstract void updateDynamicData(PhysicsWorldData data);

    protected int getSolverMaxIterations() {
        return 200;
    }

    protected float getSolverTolerance() {
        return 1e-5f;
    }

    /**
     * Main entry point. Builds/updates the matrix, solves, writes back.
     */
    public void tick(LongSet tickingSections, PhysicsWorldData data) {
        //data.resetTicked();

        M physicsMatrix = getOrBuildMatrix(tickingSections, data);
        if (physicsMatrix.size() == 0) return;
        updateDynamicData(data);

        extractFieldValues(physicsMatrix, data);

        double[] solution = ConjugateGradient.solve(
                physicsMatrix.assemblyMatrix(),
                physicsMatrix.getInitX(),          // warm start from previous tick, solution written in-place
                buildRhs(physicsMatrix),
                getSolverMaxIterations(),
                getSolverTolerance() * physicsMatrix.size(),
                physicsMatrix.cgR,
                physicsMatrix.cgP,
                physicsMatrix.cgAp
        );

        /*LeastSquare2.solve(
                physicsMatrix.assemblyMatrix(),
                buildRhs(physicsMatrix),
                getSolverMaxIterations(),
                getSolverTolerance() * physicsMatrix.size(), //so it's not decreasing real per block tolerance
                physicsMatrix.getInitX(),
                physicsMatrix.cgR,
                physicsMatrix.cgP,
                physicsMatrix.cgAtb,
                physicsMatrix.cgAp,
                physicsMatrix.cgTemp
                );*/

        physicsMatrix.setSolution(solution);
        writeBackFieldValues(physicsMatrix, data);
        data.setDirty();
    }

    /**
     * Build the right-hand side vector passed to the solver.
     * Default: just return {@code matrix.sourceVector()} directly.
     * Override (e.g. in a diffusion solver) to combine source + current field.
     */
    protected double[] buildRhs(M matrix) {
        return matrix.sourceVector();
    }

    protected M getOrBuildMatrix(LongSet tickingSections, PhysicsWorldData data) {
        M cached = getCachedMatrix(data);

        if (cached == null) {
            M built = buildUnifiedMatrix(tickingSections, data);
            setCachedMatrix(data, built);
            return built;
        }

        LongSet cachedSections = cached.sections();

        LongSet added = new LongOpenHashSet(tickingSections);
        added.removeAll(cachedSections);

        LongSet removed = new LongOpenHashSet(cachedSections);
        removed.removeAll(tickingSections);

        if (added.isEmpty() && removed.isEmpty()) {
            // Same set of sections
            return cached;
        }

        if (!removed.isEmpty()) {
            shrinkMatrix(cached, removed, data);
            if (!added.isEmpty()) {
                extendMatrix(cached, added, data);
            }
            setCachedMatrix(data, cached);
            return cached;
        }

        // Only additions — extend the existing matrix
        extendMatrix(cached, added, data);
        setCachedMatrix(data, cached);
        return cached;
    }

    /**
     * Build a brand-new matrix covering all {@code tickingSections}.
     */
    protected M buildUnifiedMatrix(LongSet tickingSections, PhysicsWorldData data) {
        int totalVoxels = tickingSections.size() * 16 * 16 * 16;

        Long2IntMap sectionToIndex = new Long2IntOpenHashMap();
        sectionToIndex.defaultReturnValue(-1);
        int idx = 0;
        for (long section : tickingSections) {
            sectionToIndex.put(section, idx);
            idx += 16 * 16 * 16;
        }

        M matrix = createMatrix(new LongOpenHashSet(tickingSections), sectionToIndex, totalVoxels);

        for (long section : tickingSections) {
            buildSectionRows(section, matrix, data);
        }

        return matrix;
    }

    /**
     * Add new sections to an existing matrix in-place.
     * The {@link PaddedCSRMatrix} simply gains new rows; indices of existing rows
     * are unchanged, so no copy is needed.
     */
    protected void extendMatrix(M matrix, LongSet addedSections, PhysicsWorldData data) {
        // Expand metadata
        int nextIdx = matrix.size();
        for (long section : addedSections) {
            matrix.sections().add(section);
            matrix.sectionToIndex().put(section, nextIdx);
            nextIdx += 16 * 16 * 16;
        }
        matrix.grow(addedSections.size() * 16 * 16 * 16);

        // Build rows for the new sections
        for (long section : addedSections) {
            buildSectionRows(section, matrix, data);
        }

        // Rebuild face neighbors that bordered the new sections —
        // they previously treated these as boundary conditions (baking T into sourceVector)
        // but now they have an interior neighbor and need off-diagonal entries instead
        for (long newSection : addedSections) {
            SectionPos pos = SectionPos.of(newSection);
            for (byte[] off : NEIGHBOR_OFFSETS) {
                long neighborSection = SectionPos.asLong(
                        pos.getX() + off[0],
                        pos.getY() + off[1],
                        pos.getZ() + off[2]
                );
                // Only rebuild if it was already in the matrix before this extension
                if (matrix.sectionToIndex().get(neighborSection) >= 0
                        && !addedSections.contains(neighborSection)) {
                    buildSectionRows(neighborSection, matrix, data);
                }
            }
        }
    }

    /**
     * Shrink the matrix in-place by removing sections using a <b>swap-with-last</b> strategy.
     *
     * <p>Rather than rebuilding all indices from scratch (O(n) row rewrites), each removed
     * section is replaced by the last section in the flat arrays in O(1) data movement:
     * <ol>
     *   <li>Copy the last section's rows and field arrays into the removed section's slot</li>
     *   <li>Rewrite column indices in those copied rows that pointed into the old last-section
     *       slot to point into the new slot</li>
     *   <li>Rewrite column indices in the 6 face-neighbors of the moved section for the same
     *       reason</li>
     *   <li>Update {@link PhysicsMatrix#sectionToIndex()} and truncate the flat arrays</li>
     * </ol>
     *
     * <p>After all removals, rebuild the face voxels of sections that bordered a removed
     * section — they previously had off-diagonal entries into the removed section which are
     * now boundary conditions and must be folded into {@code sourceVector} instead.
     *
     * <p>Cost: O(removedSections × 6 × 4096) row scans for the column-index rewrite,
     * compared to O(totalVoxels) for a full rebuild. For a typical removal of 1-3 sections
     * out of 90+ this is 10-50× faster.
     *
     * @param matrix          the matrix to shrink in-place
     * @param removedSections sections no longer in the ticking set
     * @param data            world data, needed to rebuild boundary face rows after removal
     */
    protected void shrinkMatrix(M matrix, LongSet removedSections, PhysicsWorldData data) {
        // For each removed section, swap it with the last section in the flat arrays,
        // update the index map, then truncate. No row rebuilding except for the swapped ones.

        for (long removed : removedSections) {
            int removedStart = matrix.sectionToIndex().get(removed);
            if (removedStart < 0) continue;

            // Find the last section in the current layout
            int lastStart = matrix.size() - 4096;

            if (removedStart == lastStart) {
                // Already at the end — just truncate
                matrix.sections().remove(removed);
                matrix.sectionToIndex().remove(removed);
                matrix.shrink(4096);
                continue;
            }

            // Find which section owns lastStart
            long lastSection = -1;
            for (Long2IntMap.Entry e : matrix.sectionToIndex().long2IntEntrySet()) {
                if (e.getIntValue() == lastStart) {
                    lastSection = e.getLongKey();
                    break;
                }
            }
            if (lastSection < 0) continue;

            // Copy last section's rows into removed section's slot
            PaddedCSRMatrix asm = matrix.assemblyMatrix();
            double[]        src = matrix.sourceVector();
            for (int i = 0; i < 4096; i++) {
                double[] rowValues = asm.getRowValues(lastStart + i);
                int[]    rowCols   = asm.getRowCols(lastStart + i);
                // Rewrite column indices that point into lastStart → removedStart
                for (int j = 0; j < rowCols.length; j++) {
                    if (rowCols[j] >= lastStart && rowCols[j] < lastStart + 4096) {
                        rowCols[j] = removedStart + (rowCols[j] - lastStart);
                    }
                }
                asm.setRow(removedStart + i, rowValues, rowCols, rowValues.length);
                src[removedStart + i] = src[lastStart + i];
            }
            matrix.copyFieldArrays(lastStart, removedStart, 4096); // T_current, T_next, cgRhs etc.

            // Update neighbors of the moved section — their off-diagonal column indices
            // pointing into lastStart now need to point into removedStart
            SectionPos movedPos = SectionPos.of(lastSection);
            for (byte[] off : NEIGHBOR_OFFSETS) {
                long neighborSection = SectionPos.asLong(
                        movedPos.getX() + off[0],
                        movedPos.getY() + off[1],
                        movedPos.getZ() + off[2]
                );
                int neighborStart = matrix.sectionToIndex().get(neighborSection);
                if (neighborStart < 0) continue;

                for (int i = 0; i < 4096; i++) {
                    int[]   rowCols = asm.getRowCols(neighborStart + i);
                    boolean changed = false;
                    for (int j = 0; j < rowCols.length; j++) {
                        if (rowCols[j] >= lastStart && rowCols[j] < lastStart + 4096) {
                            rowCols[j] = removedStart + (rowCols[j] - lastStart);
                            changed = true;
                        }
                    }
                    if (changed) {
                        asm.setRow(neighborStart + i, asm.getRowValues(neighborStart + i), rowCols, rowCols.length);
                    }
                }
            }

            // Remap index and truncate
            matrix.sectionToIndex().put(lastSection, removedStart);
            matrix.sectionToIndex().remove(removed);
            matrix.sections().remove(removed);
            matrix.sections().add(lastSection); // already present, no-op on LongOpenHashSet
            matrix.shrink(4096);
        }

        // Rebuild the face voxels of sections bordering removed ones —
        // they had off-diagonal entries into the removed sections that are now boundaries
        for (long removed : removedSections) {
            SectionPos pos = SectionPos.of(removed);
            for (byte[] off : NEIGHBOR_OFFSETS) {
                long neighborSection = SectionPos.asLong(
                        pos.getX() + off[0],
                        pos.getY() + off[1],
                        pos.getZ() + off[2]
                );
                int neighborStart = matrix.sectionToIndex().get(neighborSection);
                if (neighborStart < 0) continue;
                buildSectionRows(neighborSection, matrix, data);
            }
        }
    }

    /**
     * Populate (or repopulate) all 4096 rows belonging to {@code packedSection}.
     * Safe to call for both initial build and dirty-section updates.
     */
    protected void buildSectionRows(long packedSection, M matrix, PhysicsWorldData data) {
        int sectionStartIdx = matrix.sectionToIndex().get(packedSection);
        if (sectionStartIdx < 0) return;

        NeighborCache   neighbors = new NeighborCache(packedSection, data, matrix.sectionToIndex());
        PaddedCSRMatrix asm       = matrix.assemblyMatrix();
        double[]        src       = matrix.sourceVector();

        for (short z = 0; z < 16; z++) {
            for (short y = 0; y < 16; y++) {
                for (short x = 0; x < 16; x++) {
                    int globalIdx = sectionStartIdx + index3DTo1D(x, y, z);

                    // Clear old entries for this row before rebuilding
                    //Map<Integer, Double> row = asm.getRow(globalIdx);
                    //if (row != null) row.clear();
                    src[globalIdx] = 0.0;

                    buildVoxelRow(
                            x, y, z, globalIdx,
                            packedSection, sectionStartIdx,
                            data, neighbors, asm, src
                    );
                }
            }
        }
    }

    /**
     * Directly rebuild the affected rows
     */
    protected void stampVoxels(Collection<BlockPos> positions, PhysicsWorldData data) {
        M matrix = getCachedMatrix(data);
        if (matrix == null) {
            return;
        }

        Long2ObjectMap<List<BlockPos>> bySection = new Long2ObjectOpenHashMap<>();

        for (BlockPos pos : positions) {
            // the block itself
            long section = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            bySection.computeIfAbsent(section, k -> new ArrayList<>()).add(pos);

            // its 6 face neighbors
            for (byte[] offset : NEIGHBOR_OFFSETS) {
                byte     dx       = offset[0], dy = offset[1], dz = offset[2];
                BlockPos affected = pos.offset(dx, dy, dz);

                long neighborSection  = SectionPos.asLong(
                        affected.getX() >> 4,
                        affected.getY() >> 4,
                        affected.getZ() >> 4
                );

                bySection
                        .computeIfAbsent(neighborSection, k -> new ArrayList<>())
                        .add(affected);
            }
        }

        PaddedCSRMatrix asm = matrix.assemblyMatrix();
        double[]        src = matrix.sourceVector();

        for (Long2ObjectMap.Entry<List<BlockPos>> entry : bySection.long2ObjectEntrySet()) {
            long packedSection = entry.getLongKey();

            int sectionStartIdx = matrix.sectionToIndex().get(packedSection);
            if (sectionStartIdx < 0) {
                continue;
            }

            NeighborCache neighbors =
                    new NeighborCache(packedSection, data, matrix.sectionToIndex());

            for (BlockPos pos : entry.getValue()) {
                short x = (short) (pos.getX() & 15);
                short y = (short) (pos.getY() & 15);
                short z = (short) (pos.getZ() & 15);

                int globalIdx = sectionStartIdx + index3DTo1D(x, y, z);

                // clear previous row if necessary
                // asm.clearRow(globalIdx);

                src[globalIdx] = 0.0;

                buildVoxelRow(
                        x, y, z,
                        globalIdx,
                        packedSection,
                        sectionStartIdx,
                        data,
                        neighbors,
                        asm,
                        src
                );
            }
        }
    }

    /**
     * Retrieve the cached matrix from world data, or {@code null} if none.
     */
    protected abstract @Nullable M getCachedMatrix(PhysicsWorldData data);

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    protected static short index3DTo1D(int x, int y, int z) {
        return (short) ((y << 8) | (z << 4) | x);
    }

    /**
     * Populate one voxel's row in the linear system.
     *
     * <p>Write coefficients directly into {@code assemblyMatrix} via
     * {@link HashSparseMatrix#set(int, int, double)}.
     *
     * <p>For boundary voxels (neighbor not in the matrix), fold the contribution
     * into {@code sourceVector[globalIdx]} instead of adding an off-diagonal entry.
     *
     * @param x               local x (0-15)
     * @param y               local y (0-15)
     * @param z               local z (0-15)
     * @param globalIdx       row/column index in the global matrix
     * @param packedSection   packed long form of sectionPos
     * @param sectionStartIdx first global index of this section
     * @param data            world data
     * @param neighbors       preloaded neighbor layer cache
     * @param assemblyMatrix  matrix to write off-diagonal entries into
     * @param sourceVector    RHS vector; add source contributions here
     */
    protected abstract void buildVoxelRow(
            short x, short y, short z,
            int globalIdx,
            long packedSection,
            int sectionStartIdx,
            PhysicsWorldData data,
            NeighborCache neighbors,
            PaddedCSRMatrix assemblyMatrix,
            double[] sourceVector
    );

    /**
     * Base container for physics state.
     *
     * <p>Holds the mutable {@link HashSparseMatrix} used during assembly and
     * the source vector {@code b}. Concrete subclasses add field vectors
     * (e.g. {@code T_current}, {@code T_next}).
     *
     * <p>{@link #grow} is called when new sections are appended; subclasses must
     * extend their own field arrays accordingly.
     */
    public abstract static class PhysicsMatrix {
        private final LongSet     sections;
        private final Long2IntMap sectionToIndex;
        private final int         maxNnzPerRow;
        public        double[]    cgRhs;
        /**
         * Solver working arrays — allocated once, reused every tick, grown with the matrix.
         *
         * <p>The CG solver on normal equations needs four scratch vectors per solve.
         * At 92 sections x 4096 voxels = 376 832 doubles each, allocating these fresh
         * every tick costs ~12 MB per call and ~240 MB/s at 20 ticks/s, creating severe
         * GC pressure. Owning them here eliminates all per-tick allocation for the solver.
         *
         * <ul>
         *   <li>{@code cgR}    — residual vector,      length = size</li>
         *   <li>{@code cgP}    — search direction,     length = size</li>
         *   <li>{@code cgAp}   — AtA·p accumulator,    length = size</li>
         *   <li>{@code cgTemp} — intermediate A·p,     length = size</li>
         * </ul>
         * <p>
         * Pass these to {@link LeastSquare#solve} via the overload that accepts
         * pre-allocated working buffers.
         */
        double[] cgR;
        double[] cgP;
        double[] cgAtb;
        double[] cgAp;
        double[] cgTemp;
        private PaddedCSRMatrix assemblyMatrix;
        private double[]        sourceVector;
        private int             size;


        protected PhysicsMatrix(LongSet sections, Long2IntMap sectionToIndex, int size, int maxNnzPerRow) {
            this.sections = sections;
            this.sectionToIndex = sectionToIndex;
            this.size = size;
            this.maxNnzPerRow = maxNnzPerRow;
            this.assemblyMatrix = new PaddedCSRMatrix(size, size, maxNnzPerRow);
            this.sourceVector = new double[size];
            this.cgR = new double[size];
            this.cgP = new double[size];
            this.cgAp = new double[size];
            this.cgAtb = new double[size];
            this.cgTemp = new double[size];
            this.cgRhs = new double[size];

        }

        public LongSet sections() {
            return sections;
        }

        public Long2IntMap sectionToIndex() {
            return sectionToIndex;
        }

        public PaddedCSRMatrix assemblyMatrix() {
            return assemblyMatrix;
        }

        public double[] sourceVector() {
            return sourceVector;
        }

        public int size() {
            return size;
        }

        // In PhysicsMatrix
        final void shrink(int removedVoxels) {
            int newSize = this.size - removedVoxels;
            this.assemblyMatrix = assemblyMatrix.resize(newSize);
            this.sourceVector   = Arrays.copyOf(sourceVector, newSize);
            this.cgR            = Arrays.copyOf(cgR,   newSize);
            this.cgP            = Arrays.copyOf(cgP,   newSize);
            this.cgAp           = Arrays.copyOf(cgAp,  newSize);
            this.cgAtb          = Arrays.copyOf(cgAtb, newSize);
            this.cgRhs          = Arrays.copyOf(cgRhs, newSize);
            this.cgTemp          = Arrays.copyOf(cgTemp, newSize);
            this.size           = newSize;
            onShrink(newSize);
        }

        /**
         * Subclasses truncate their field arrays here.
         */
        protected abstract void onShrink(int newSize);

        /**
         * Subclasses copy their field arrays (T_current, T_next, etc.) from src slot to dst slot.
         */
        protected abstract void copyFieldArrays(int srcStart, int dstStart, int count);

        /**
         * Called when new sections are added without a full rebuild.
         * Replaces {@link #assemblyMatrix} and {@link #sourceVector} with
         * larger copies, then calls {@link #onGrow} so subclasses can extend
         * their own arrays.
         */
        final void grow(int additionalVoxels) {
            int newSize = this.size + additionalVoxels;
            this.assemblyMatrix = assemblyMatrix.resize(newSize);
            this.sourceVector   = Arrays.copyOf(sourceVector, newSize);
            this.cgR            = Arrays.copyOf(cgR,   newSize);
            this.cgP            = Arrays.copyOf(cgP,   newSize);
            this.cgAp           = Arrays.copyOf(cgAp,  newSize);
            this.cgAtb          = Arrays.copyOf(cgAtb, newSize);
            this.cgRhs          = Arrays.copyOf(cgRhs, newSize);
            this.cgTemp          = Arrays.copyOf(cgTemp, newSize);
            this.size           = newSize;
            onGrow(newSize);
        }

        /**
         * Called after {@link #grow}. Subclasses extend their field arrays here.
         *
         * @param newSize the updated total voxel count
         */
        protected abstract void onGrow(int newSize);

        /**
         * Store the solver's output. Called after every solve.
         *
         * @param solution the solution vector returned by the linear solver
         */
        public abstract void setSolution(double[] solution);

        public abstract double[] getInitX();
    }

    //TODO would be best to have this as a mutable class, that way there is less impact on the garbage collector

    /**
     * Per-section cache of data layers for the center section and its 26 neighbors.
     * Prevents repeated map lookups inside the inner voxel loop.
     */
    protected class NeighborCache {
        //use an array of size 7 with the 6 first as neighbor (use the same mapping as neighbor_offset)
        // and last as center
        private final AbstractDataLayer[][] layerCache;

        private final boolean[] inMatrix      = new boolean[7];
        private final int[]     globalIndices = new int[7];

        public NeighborCache(long center, PhysicsWorldData data, Long2IntMap sectionToIndex) {
            DataLayerType[] needed = getRequiredLayers();
            this.layerCache = new AbstractDataLayer[7][needed.length];

            int i = 0;
            for (byte[] offset : NEIGHBOR_OFFSETS) {
                int  dx  = offset[0], dy = offset[1], dz = offset[2];
                long sec = SectionPos.offset(center, dx, dy, dz);

                layerCache[i] = data.getLayers(sec, needed);
                inMatrix[i] = sectionToIndex.containsKey(sec);
                globalIndices[i] = sectionToIndex.get(sec);
                i++;
            }

            layerCache[i] = data.getLayers(center, needed);
            inMatrix[i] = sectionToIndex.containsKey(center);
            globalIndices[i] = sectionToIndex.get(center);
        }

        public static int getIndex(int nx, int ny, int nz) {
            return nx == 16 ? 0 : nx == -1 ? 1 : ny == 16 ? 2 : ny == -1 ? 3 : nz == 16 ? 4 : nz == -1 ? 5 : 6;
        }

        public @Nullable AbstractDataLayer getLayer(int section, int type) {
            return layerCache[section][type];
        }

        public boolean isInMatrix(int section) {
            return inMatrix[section];
        }

        public int globalIndex(int section) {
            return globalIndices[section];
        }
    }
}