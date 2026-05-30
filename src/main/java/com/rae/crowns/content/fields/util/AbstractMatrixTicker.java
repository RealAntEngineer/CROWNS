package com.rae.crowns.content.fields.util;


import com.rae.crowns.content.fields.temperature.ConductionDataLayer;
import com.rae.crowns.content.fields.temperature.MutableCSRMatrix;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implement everything needed to do a voxel simulation.
 * stamping is what the child is taking charge of, the updating of the matrix, the shrinking and expansion...
 */
public abstract class AbstractMatrixTicker {

    protected static final int[][] OFFSETS     = new int[][]{
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };        // 6 directions: +x, -x, +y, -y, +z, -z

    /**
     * Stores the unified thermal matrix and metadata
     *
     * @param sections       Which sections this matrix covers
     * @param sectionToIndex Section -> starting index in vectors
     * @param A         The mutable A matrix
     * @param hrs              Source vector
     * @param X_current      Work buffer for current temps
     * @param X_next         Work buffer for next temps
     * @param size           Total number of nodes
     */
    public record PhysicsMatrix(LongSet sections, Long2IntMap sectionToIndex, MutableCSRMatrix A, double[] hrs,
                                double[] X_current, double[] X_next, int size) {
    }



    /**
     * Information about a neighbor voxel
     */
    public record NeighborInfo(
            float conductivity,
            float temperature,
            int globalIndex,      // Index in global matrix (-1 if not in matrix)
            boolean isInMatrix    // True if neighbor is part of ticking sections
    ) {
    }

    /**
     * Cache for neighbor section data to avoid repeated lookups
     */
    public static class NeighborCache {
        private final Long2IntMap                     sectionToIndex;
        //first 6 are neighbors, and the 7th is the center
        private final ConductionDataLayer[]  condCache = new ConductionDataLayer[7];
        private final TemperatureDataLayer[] tempCache = new TemperatureDataLayer[7];

        public NeighborCache(SectionPos center, PhysicsWorldData data, Long2IntMap sectionToIndex) {
            this.sectionToIndex = sectionToIndex;

            // Preload center and 6 neighbors
            int dx, dy, dz;
            int i = 0;
            for (int[] offsets : OFFSETS) {
                dx = offsets[0];
                dy = offsets[1];
                dz = offsets[2];
                long section = SectionPos.asLong(
                        center.getX() + dx,
                        center.getY() + dy,
                        center.getZ() + dz
                );

                ConductionDataLayer  cond = data.getLayer(section, DataLayerType.CONDUCTION);
                TemperatureDataLayer temp = data.getLayer(section, DataLayerType.TEMPERATURE);

                if (cond != null) condCache[i] = cond;
                if (temp != null) tempCache[i] = temp;
            }
            long section = SectionPos.asLong(
                    center.getX(),
                    center.getY(),
                    center.getZ()
            );

            ConductionDataLayer  cond = data.getLayer(section, DataLayerType.CONDUCTION);
            TemperatureDataLayer temp = data.getLayer(section, DataLayerType.TEMPERATURE);

            if (cond != null) condCache[6] = cond;
            if (temp != null) tempCache[6] = temp;
        }

        public ConductionDataLayer getSectionConduction(int dirIndex) {
            return condCache[dirIndex];
        }

        public TemperatureDataLayer getSectionTemperature(int dirIndex) {
            return tempCache[dirIndex];
        }

        public int getSectionStartIndex(long section) {
            return sectionToIndex.get(section);
        }
    }
}