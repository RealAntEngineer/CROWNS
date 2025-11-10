package com.rae.crowns.content.fields.util;

public abstract class AbstractDataLayer {
    public static final int SIZE = 16 * 16 * 16;

    /** Calculates a linear index from 3D coordinates (0–15). */
    protected static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    /** Decode stored value at index to float. */
    protected abstract float decode(int index);

    /** Encode float value into stored representation. */
    protected abstract void encode(int index, float value);

    /**
     * Deserialize data from bytes.
     *
     * @return child
     */
    public abstract AbstractDataLayer fromBytes(byte[] bytes);

    /** Serialize to bytes. */
    public abstract byte[] toBytes();

    /** Get value at (x, y, z). */
    public float get(int x, int y, int z) {
        return decode(index(x, y, z));
    }

    /** Set value at (x, y, z). */
    public void set(int x, int y, int z, float value) {
        encode(index(x, y, z), value);
    }
}
