package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.temperature.PaddedCSRMatrix;
import com.rae.formicapi.fondation.math.operators.CSRMatrix;
import com.rae.formicapi.fondation.math.operators.HashSparseMatrix;
import com.rae.formicapi.fondation.math.solvers.LeastSquare;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Abstract matrix-based physics solver.
 *
 * <p>Handles all the infrastructure for building and solving sparse linear systems
 * across voxel grids. Subclasses only implement the physics: field I/O and per-voxel
 * equation coefficients.
 *
 * <p>Assembly uses {@link HashSparseMatrix} for random-access writes. Once all rows
 * are populated, the matrix is compiled to {@link CSRMatrix} for efficient solving.
 * The {@link HashSparseMatrix} is retained between ticks so dirty sections can be
 * patched incrementally without a full rebuild.
 *
 * <p>Section lifecycle:
 * <ul>
 *   <li>No change → patch dirty rows only, recompile to CSR</li>
 *   <li>Sections added → fill new rows, recompile</li>
 *   <li>Sections removed or mixed → full rebuild</li>
 * </ul>
 *
 * @param <M> concrete subclass of {@link PhysicsMatrix}
 */
public abstract class AbstractMatrixPhysicsSolver<M extends AbstractMatrixPhysicsSolver.PhysicsMatrix> {

    protected static final int[][] NEIGHBOR_OFFSETS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    // -------------------------------------------------------------------------
    // Abstract — physics specific
    // -------------------------------------------------------------------------

    protected static int index3DTo1D(int x, int y, int z) {
        return x + y * 16 + z * 256;
    }

    /**
     * Timestep used by this solver.
     */
    protected abstract float getTimeStep();

    /**
     * Data layer types needed for neighbor lookups (preloaded into {@link NeighborCache}).
     */
    protected abstract DataLayerType[] getRequiredLayers();

    /**
     * Allocate a new, empty physics matrix of the concrete type.
     */
    protected abstract M createMatrix(LongSet sections, Long2IntMap sectionToIndex, int totalSize);

    /**
     * Retrieve the cached matrix from world data, or {@code null} if none.
     */
    protected abstract M getCachedMatrix(PhysicsWorldData data);

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

    // -------------------------------------------------------------------------
    // Solver hooks — override to tune
    // -------------------------------------------------------------------------

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
            int x, int y, int z,
            int globalIdx,
            long packedSection,
            int sectionStartIdx,
            PhysicsWorldData data,
            NeighborCache neighbors,
            PaddedCSRMatrix assemblyMatrix,
            double[] sourceVector
    );

    protected int getSolverMaxIterations() {
        return 200;
    }

    // -------------------------------------------------------------------------
    // Core tick
    // -------------------------------------------------------------------------

    protected float getSolverTolerance() {
        return 1e-1f;
    }

    /**
     * Main entry point. Builds/updates the matrix, solves, writes back.
     */
    public void tick(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
        data.resetTicked();
        updateDynamicData(data);

        M physicsMatrix = getOrBuildMatrix(tickingSections, data);
        if (physicsMatrix == null || physicsMatrix.size() == 0) return;

        extractFieldValues(physicsMatrix, data);

        // Compile to CSR and solve
        double[] solution = LeastSquare.solve(
                physicsMatrix.assemblyMatrix(),
                buildRhs(physicsMatrix),
                getSolverMaxIterations(),
                getSolverTolerance()
        );

        physicsMatrix.setSolution(solution);
        writeBackFieldValues(physicsMatrix, data);
        data.setDirty();
    }

    // -------------------------------------------------------------------------
    // Matrix lifecycle
    // -------------------------------------------------------------------------

    /**
     * Build the right-hand side vector passed to the solver.
     * Default: just return {@code matrix.sourceVector()} directly.
     * Override (e.g. in a diffusion solver) to combine source + current field.
     */
    protected double[] buildRhs(M matrix) {
        return matrix.sourceVector();
    }

    protected M getOrBuildMatrix(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
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
            // Same set of sections — patch only dirty ones
            for (long section : tickingSections) {
                if (data.isDirty(section)) {
                    buildSectionRows(section, cached, data);
                }
            }
            return cached;
        }

        if (!removed.isEmpty()) {
            // Any removal requires a full rebuild (indices shift)
            M rebuilt = buildUnifiedMatrix(tickingSections, data);
            setCachedMatrix(data, rebuilt);
            return rebuilt;
        }

        // Only additions — extend the existing matrix
        extendMatrix(cached, added, tickingSections, data);
        setCachedMatrix(data, cached);
        return cached;
    }

    /**
     * Build a brand-new matrix covering all {@code tickingSections}.
     */
    protected M buildUnifiedMatrix(@NotNull LongSet tickingSections, @NotNull PhysicsWorldData data) {
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
     * The {@link HashSparseMatrix} simply gains new rows; indices of existing rows
     * are unchanged, so no copy is needed.
     */
    protected void extendMatrix(M matrix, LongSet addedSections, LongSet allSections, PhysicsWorldData data) {
        // Expand metadata
        int nextIdx = matrix.size();
        for (long section : addedSections) {
            matrix.sections().add(section);
            matrix.sectionToIndex().put(section, nextIdx);
            nextIdx += 16 * 16 * 16;
        }
        matrix.grow(addedSections.size() * 16 * 16 * 16);

        // Populate new rows
        for (long section : addedSections) {
            buildSectionRows(section, matrix, data);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
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
     * Mark all sections within 1 block of each position as dirty,
     * so their matrix rows are rebuilt next tick.
     */
    protected void stampVoxels(List<BlockPos> positions, PhysicsWorldData data) {
        Set<Long> affected = new HashSet<>();
        for (BlockPos pos : positions) {
            int sx = pos.getX() >> 4;
            int sy = pos.getY() >> 4;
            int sz = pos.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++)
                        affected.add(SectionPos.asLong(sx + dx, sy + dy, sz + dz));
        }
        affected.forEach(data::setDirty);
    }

    // -------------------------------------------------------------------------
    // Nested types
    // -------------------------------------------------------------------------

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
        private final LongSet          sections;
        private final Long2IntMap     sectionToIndex;
        private       PaddedCSRMatrix assemblyMatrix;
        private       double[]        sourceVector;
        private       int              size;

        protected PhysicsMatrix(LongSet sections, Long2IntMap sectionToIndex, int size) {
            this.sections = sections;
            this.sectionToIndex = sectionToIndex;
            this.size = size;
            this.assemblyMatrix = new PaddedCSRMatrix(size, size, 7);
            this.sourceVector = new double[size];
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

        /**
         * Called when new sections are added without a full rebuild.
         * Replaces {@link #assemblyMatrix} and {@link #sourceVector} with
         * larger copies, then calls {@link #onGrow} so subclasses can extend
         * their own arrays.
         */
        final void grow(int additionalVoxels) {
            int newSize = this.size + additionalVoxels;

            // Rebuild HashSparseMatrix at the new size, copying old entries
            PaddedCSRMatrix newAsm = new PaddedCSRMatrix(newSize, newSize, 7);
            for (int r = 0; r < size; r++) {
                double[] rowValues = assemblyMatrix.getRowValues(r);
                int[] rowCols = assemblyMatrix.getRowCols(r);
                newAsm.setRow(r, rowValues, rowCols);
            }

            double[] newSrc = Arrays.copyOf(sourceVector, newSize);

            this.assemblyMatrix = newAsm;
            this.sourceVector = newSrc;
            this.size = newSize;

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
            for (int[] offset : NEIGHBOR_OFFSETS) {
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

        public AbstractDataLayer getLayer(int section, int type) {
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