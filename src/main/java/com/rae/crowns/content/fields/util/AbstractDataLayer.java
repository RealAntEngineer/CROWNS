package com.rae.crowns.content.fields.util;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractDataLayer {
    public static final int SIZE = 16 * 16 * 16;

    /**
     * Deserialize data from bytes.
     *
     * @return child
     */
    public abstract @NotNull AbstractDataLayer fromBytes(byte[] bytes);

    /**
     * Serialize to bytes.
     */
    public abstract byte[] toBytes();

    /**
     * Get value at (x, y, z).
     */
    public float get(int x, int y, int z) {
        return decode(index(x, y, z));
    }

    /**
     * Decode stored value at index to float.
     */
    protected abstract float decode(int index);

    /**
     * Calculates a linear index from 3D coordinates (0–15).
     */
    protected static int index(int x, int y, int z) {
        if (x < 0 || x > 15 || y < 0 || y > 15 || z < 0 || z > 15)
            throw new IllegalStateException("Invalid voxel coord: " + x + "," + y + "," + z);
        return (y << 8) | (z << 4) | x;
    }

    /**
     * Set value at (x, y, z).
     */
    public void set(int x, int y, int z, float value) {
        encode(index(x, y, z), value);
    }

    /**
     * Encode float value into stored representation.
     */
    protected abstract void encode(int index, float value);
}
