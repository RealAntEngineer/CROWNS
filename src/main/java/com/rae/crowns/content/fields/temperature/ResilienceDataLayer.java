package com.rae.crowns.content.fields.temperature;

import net.minecraft.util.Mth;

import java.nio.ByteBuffer;

public class ResilienceDataLayer {
    public static final int SIZE = 16 * 16 * 16;
    private final byte[] data;

    public ResilienceDataLayer() {
        this.data = new byte[SIZE];
    }

    public static ResilienceDataLayer fromBytes(byte[] bytes) {
        ResilienceDataLayer temp = new ResilienceDataLayer();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) temp.data[i] = buffer.get();
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

    /** Encode 0..1 -> byte [-128..127] */
    public void set(int x, int y, int z, float resilience) {
        int scaled = Math.round(Mth.clamp(resilience, 0f, 1f) * 255f);
        data[index(x, y, z)] = (byte)(scaled - 128);
    }

    /** Decode byte [-128..127] -> float 0..1 */
    public float get(int x, int y, int z) {
        return (data[index(x, y, z)] + 128) / 255f;
    }

    public byte[] getRaw() {
        return data;
    }
}