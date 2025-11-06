package com.rae.crowns.content.fields.temperature;


import org.jetbrains.annotations.NotNull;

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
public class TemperatureDataLayer extends AbstractDataLayer {
    public static final double SCALE = 100000f; // 5 decimal places
    public static final double MIN_TEMPERATURE = 0.0d;
    public static final double MAX_TEMPERATURE =
            (Integer.MAX_VALUE - (long) Integer.MIN_VALUE) / SCALE; // ≈ 42949.67295
    private final int[] data = new int[SIZE];
    //private final int[] defaultData = new int[SIZE];

    @Override
    public TemperatureDataLayer fromBytes(byte @NotNull [] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) data[i] = buffer.getInt();
        //for (int i = 0; i < SIZE; i++) defaultData[i] = buffer.getInt();
        return this;
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * 4);// * 2);
        for (int val : data) buffer.putInt(val);
        //for (int val : defaultData) buffer.putInt(val);
        return buffer.array();
    }

    @Override
    protected float decode(int index) {
        int stored = data[index];
        return (float) ((stored - (long) Integer.MIN_VALUE) / SCALE);
    }

    @Override
    protected void encode(int index, float value) {
        long encoded = (long) (value * SCALE) + (long) Integer.MIN_VALUE;
        data[index] = (int) encoded;
    }

    /*public float getDefault(int x, int y, int z) {
        int stored = defaultData[index(x, y, z)];
        return (float) ((stored - (long) Integer.MIN_VALUE) / SCALE);
    }

    public void setDefault(int x, int y, int z, float temperature) {
        long encoded = (long) (temperature * SCALE) + (long) Integer.MIN_VALUE;
        defaultData[index(x, y, z)] = (int) encoded;
    }*/
}