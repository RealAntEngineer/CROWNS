package com.rae.crowns.content.fields.temperature;

import net.minecraft.core.Direction;

import java.util.Set;
import java.util.function.Function;

/**
 * Generic section iteration helper.
 * - Section size is assumed 16 (voxel coords 0..15).
 * - Provides neighbor resolution across section boundaries.
 */
public final class SectionLooper {
    public static final int SECTION_SIZE = 16;

    public interface VoxelVisitor {
        /**
         * Called for every voxel.
         *
         * @param packedSection the section packed id
         * @param sx section X
         * @param sy section Y
         * @param sz section Z
         * @param x  local x (0..15)
         * @param y  local y (0..15)
         * @param z  local z (0..15)
         * @param ctx helper to resolve neighbors / absolute pos
         */
        void visit(long packedSection, int sx, int sy, int sz, int x, int y, int z, Context ctx);
    }

    /**
     * Helper object given to visitor for neighbor resolution and constants.
     * Lightweight — can be reused by visitor.
     */
    public static final class Context {
        private int sx, sy, sz;
        private short packedXYZ; // stores x, y, z as 4 bits each (0-15)
        private AbstractDataLayer[] layers;

        /**
         * Mutable constructor for reuse. Initializes with zeros.
         */
        public Context() {
            this.sx = 0;
            this.sy = 0;
            this.sz = 0;
            this.packedXYZ = 0;
        }

        /**
         * Set section coordinates
         */
        public void section(int sx, int sy, int sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
        }

        /**
         * Set local voxel coordinates inside the section (0-15)
         */
        public void pos(int x, int y, int z) {
            this.packedXYZ = (short) ((x & 15) << 8 | (y & 15) << 4 | (z & 15));
        }

        /**
         * Getters for local coordinates
         */
        public int x() {
            return (packedXYZ >> 8) & 15;
        }

        public int y() {
            return (packedXYZ >> 4) & 15;
        }

        public int z() {
            return packedXYZ & 15;
        }

        /**
         * Compute absolute BlockPos (packed as a long)
         */
        public long packedPos() {
            return packSection((sx << 4) + x(), (sy << 4) + y(), (sz << 4) + z());//the packing is the same for blocks and sections
        }

        /**
         * Resolves a neighbor coordinate potentially outside the local section.
         * Returns NeighborRef with packed section and local coordinates inside that section.
         */
        public NeighborRef resolveNeighbor(int nx, int ny, int nz) {
            int nsx = sx, nsy = sy, nsz = sz;
            int lx = nx, ly = ny, lz = nz;

            if (nx < 0) {
                nsx--;
                lx += SECTION_SIZE;
            } else if (nx >= SECTION_SIZE) {
                nsx++;
                lx -= SECTION_SIZE;
            }

            if (ny < 0) {
                nsy--;
                ly += SECTION_SIZE;
            } else if (ny >= SECTION_SIZE) {
                nsy++;
                ly -= SECTION_SIZE;
            }

            if (nz < 0) {
                nsz--;
                lz += SECTION_SIZE;
            } else if (nz >= SECTION_SIZE) {
                nsz++;
                lz -= SECTION_SIZE;
            }

            long packed = SectionLooper.packSection(nsx, nsy, nsz);
            return new NeighborRef(packed, lx, ly, lz);
        }

        /**
         * Convenience: loop over axis-aligned neighbor offsets using Direction.values()
         */
        public interface NeighborConsumer {
            void accept(int nx, int ny, int nz, NeighborRef ref);
        }
        // inside Context
        private static final int[] NEIGHBOR_OFFSETS = {
                1, 0, 0,   // EAST
                -1, 0, 0,   // WEST
                0, 1, 0,   // UP
                0,-1, 0,   // DOWN
                0, 0, 1,   // SOUTH
                0, 0,-1    // NORTH
        };

        /**
         * Iterates all 6 neighbors of the current voxel, without Direction enums or allocations.
         */
        public void forEachNeighbor(NeighborConsumer consumer) {
            int cx = x();
            int cy = y();
            int cz = z();

            for (int i = 0; i < 6; i++) {
                int nx = cx + NEIGHBOR_OFFSETS[i * 3];
                int ny = cy + NEIGHBOR_OFFSETS[i * 3 + 1];
                int nz = cz + NEIGHBOR_OFFSETS[i * 3 + 2];
                NeighborRef ref = resolveNeighbor(nx, ny, nz);
                consumer.accept(nx, ny, nz, ref);
            }
        }

        public void setLayers(AbstractDataLayer[] layers) {
            this.layers = layers;
        }

        public float getData(int idx) {
            return layers[idx].get(x(), y(), z());
        }

        public void setData(int idx, float value) {
            layers[idx].set(x(), y(), z(), value);
        }

    }

    /**
     * result of neighbor resolution
     */
        public record NeighborRef(long packedSection, int localX, int localY, int localZ) {
    }

    // --- packing/unpacking kept identical to your original scheme ---
    public static long packSection(int sx, int sy, int sz) {
        return ((long)sx & 0x3FFFFF) << 42
                | ((long)sz & 0x3FFFFF) << 20
                | ((long)sy & 0xFFFFF);
    }

    public static int unpackSectionX(long packed) { return (int)(packed >> 42); }
    public static int unpackSectionY(long packed) { return (int)(packed << 44 >> 44); }
    public static int unpackSectionZ(long packed) { return (int)(packed << 22 >> 42); }

    /**
     * Iterate every voxel in every packedSection and call visitor.
     * Uses a single mutable Context per section to avoid allocations.
     */
    public static void iterate(Set<Long> tickingSections, Function<Long, AbstractDataLayer[]> layerGetter, VoxelVisitor visitor) {
        Context ctx = new Context(); // single reusable context per iterate call

        for (long packedSection : tickingSections) {
            int sx = unpackSectionX(packedSection);
            int sy = unpackSectionY(packedSection);
            int sz = unpackSectionZ(packedSection);

            ctx.section(sx, sy, sz); // set section coordinates once

            // --- initialize layers once per section ---
            AbstractDataLayer[] layers = layerGetter.apply(packedSection);

            // --- skip the section if any layer is null ---
            boolean skip = false;
            if (layers == null) continue;
            for (AbstractDataLayer layer : layers) {
                if (layer == null) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;

            ctx.setLayers(layers);

            // --- iterate voxels using single-short loop ---
            for (short i = 0; i < 4096; i++) {
                int x = (i >> 8) & 15;
                int y = (i >> 4) & 15;
                int z = i & 15;

                ctx.pos(x, y, z); // only update voxel position

                visitor.visit(packedSection, sx, sy, sz, x, y, z, ctx);
            }
        }
    }
}
