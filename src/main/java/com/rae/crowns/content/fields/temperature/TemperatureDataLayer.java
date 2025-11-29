package com.rae.crowns.content.fields.temperature;


import com.rae.crowns.content.fields.util.AbstractDataLayer;
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
    public static final double SCALE = 10f; // 1 decimal places
    public static final double MIN_TEMPERATURE = 0.0d;
    public static final double MAX_TEMPERATURE =
            (Short.MAX_VALUE - (long) Short.MIN_VALUE) / SCALE; // ≈ 42949.67295
    private final float[] data = new float[SIZE];
    //private final int[] defaultData = new int[SIZE];

    @Override
    public TemperatureDataLayer fromBytes(byte @NotNull [] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) data[i] = (float) ((buffer.getShort() - Short.MIN_VALUE)/SCALE);
        //for (int i = 0; i < SIZE; i++) defaultData[i] = buffer.getInt();
        return this;
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * 2);// * 2);
        for (float value : data) buffer.putShort((short) ((value * SCALE) + Short.MIN_VALUE));
        //for (int val : defaultData) buffer.putInt(val);
        return buffer.array();
    }

    @Override
    public float get(int x, int y, int z) {
        return data[index(x, y, z)];
    }

    @Override
    public void set(int x, int y, int z, float value) {
        data[index(x, y, z)] = value;
    }

    @Override
    protected float decode(int index) {
        return 0;
    }

    @Override
    protected void encode(int index, float value) {
    }
}