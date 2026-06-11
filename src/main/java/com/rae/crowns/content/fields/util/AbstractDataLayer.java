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
    public float get(short x, short y, short z) {
        return getDirect(index(x, y, z));
    }

    public float getDirect(short idx) {
        return decode(idx);
    }

    /**
     * Calculates a linear index from 3D coordinates (0–15). it's x + z * 16 + y * 256
     */
    public static short index(short x, short y, short z) {
        if (x < 0 || x > 15 || y < 0 || y > 15 || z < 0 || z > 15)
            throw new IllegalStateException("Invalid voxel coord: " + x + "," + y + "," + z);
        return (short) ((y << 8) | (z << 4) | x);
    }

    /**
     * Decode stored value at index to float.
     */
    protected abstract float decode(short index);

    /**
     * Set value at (x, y, z).
     */
    public void set(short x, short y, short z, float value) {
        setDirect(index(x, y, z), value);
    }

    public void setDirect(short idx, float value) {
        encode(idx, value);
    }

    /**
     * Encode float value into stored representation.
     */
    protected abstract void encode(short index, float value);
}
