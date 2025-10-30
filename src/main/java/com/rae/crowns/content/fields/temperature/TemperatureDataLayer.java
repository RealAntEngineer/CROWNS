package com.rae.crowns.content.fields.temperature;

import net.minecraft.util.Mth;

import java.nio.ByteBuffer;

/**
 * Temperature data for a Section (16×16×16)
 * <p>
 * Temperatures are stored as fixed-point integers with 5 decimal digits of precision.
 * The int range (-2_147_483_648 to 2_147_483_647) is mapped linearly to temperature space
 * by offsetting with Integer.MIN_VALUE.
 * <p>
 * Encoding:
 *   stored = (int)(temperature * SCALE) + Integer.MIN_VALUE
 * <p>
 * Decoding:
 *   temperature = (stored - Integer.MIN_VALUE) / SCALE
 */
public class TemperatureDataLayer {
    public static final int SIZE = 16 * 16 * 16;
    public static final double SCALE = 100000f; // 5 decimal places

    public static final double MIN_TEMPERATURE = 0.0d;
    public static final double MAX_TEMPERATURE =
            (Integer.MAX_VALUE - (long) Integer.MIN_VALUE) / SCALE; // ≈ 42949.67295
    private final int[] data;
    private final int[] defaultData;

    public TemperatureDataLayer() {
        this.data = new int[SIZE];
        this.defaultData = new int[SIZE];
    }

    public static TemperatureDataLayer fromBytes(byte[] bytes) {
        TemperatureDataLayer temp = new TemperatureDataLayer();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        for (int i = 0; i < SIZE; i++) {
            temp.data[i] = buffer.getInt();
        }
        for (int i = 0; i < SIZE; i++) {
            temp.defaultData[i] = buffer.getInt();
        }

        return temp;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * 4 * 2);
        for (int val : data) {
            buffer.putInt(val);
        }
        for (int val : defaultData) {
            buffer.putInt(val);
        }
        return buffer.array();
    }

    public int[] getRaw() {
        return data;
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    public float get(int x, int y, int z) {
        int stored = data[index(x, y, z)];
        return (float) ((stored - (long) Integer.MIN_VALUE) / SCALE);
    }

    public float getDefault(int x, int y, int z) {
        int stored = defaultData[index(x, y, z)];
        return (float) ((stored - (long) Integer.MIN_VALUE) / SCALE);
    }

    public void set(int x, int y, int z, float temperature) {
        long encoded = (long) (temperature * SCALE) + (long) Integer.MIN_VALUE;
        //encoded = Math.clamp(encoded, Integer.MIN_VALUE, Integer.MAX_VALUE);
        data[index(x, y, z)] = (int) encoded;
    }

    public void setDefault(int x, int y, int z, float temperature) {
        long encoded = (long) (temperature * SCALE) + (long) Integer.MIN_VALUE;
        //encoded = Mth.clamp(encoded, Integer.MIN_VALUE, Integer.MAX_VALUE);
        defaultData[index(x, y, z)] = (int) encoded;
    }
}