package com.rae.crowns.content.fields.temperature;

import net.minecraft.util.Mth;

import java.nio.ByteBuffer;

/**
 * Conduction coefficient for a Section (16×16×16)
 * Stored as 8-bit mini-float: 2-bit mantissa + 6-bit signed exponent (-16 → +47)
 * Uses bit-shifts instead of Math.pow for speed.
 */
public class ConductionDataLayer {
    public static final int SIZE = 16 * 16 * 16;
    private final byte[] data;

    public static final float MIN_VALUE = 1.0f / (1 << 16);      // 2^-16
    public static final float MAX_VALUE = 1.75f * (1L << 47);    // 1.75 * 2^47

    public ConductionDataLayer() {
        this.data = new byte[SIZE];
    }

    public static ConductionDataLayer fromBytes(byte[] bytes) {
        ConductionDataLayer temp = new ConductionDataLayer();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) {
            temp.data[i] = buffer.get();
        }
        return temp;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        for (byte val : data) buffer.put(val);
        return buffer.array();
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    /** Decode the mini-float using bit-shifts */
    public float get(int x, int y, int z) {
        int b = data[index(x, y, z)] & 0xFF;
        int mantissa = b & 0b11;            // 2-bit mantissa
        int exponent = ((b >> 2) & 0b111111) - 16; // signed exponent: -16 -> +47

        float m = 1.0f + mantissa / 4.0f;

        if (exponent >= 0) {
            return m * (1L << exponent);
        } else {
            return m / (1L << -exponent);
        }
    }

    /** Encode a float into mini-float using bit-shifts */
    public void set(int x, int y, int z, float value) {
        float clamped = Mth.clamp(value, MIN_VALUE, MAX_VALUE);

        // compute exponent
        int exponent = (int) Math.floor(Math.log(clamped) / Math.log(2));
        exponent = Mth.clamp(exponent, -16, 47);

        // compute mantissa
        float normalized;
        if (exponent >= 0) normalized = clamped / (1L << exponent);
        else normalized = clamped * (1L << -exponent);

        int mantissa = Mth.clamp(Math.round((normalized - 1f) * 4f), 0, 3);

        int stored = ((exponent + 16) << 2) | mantissa;
        data[index(x, y, z)] = (byte) stored;
    }
}
