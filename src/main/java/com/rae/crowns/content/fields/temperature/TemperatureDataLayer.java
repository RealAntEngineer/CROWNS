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
 * stored = (int)(temperature * SCALE) + Integer.MIN_VALUE
 * <p>
 * Decoding:
 * temperature = (stored - Integer.MIN_VALUE) / SCALE
 */
public class TemperatureDataLayer extends AbstractDataLayer {
    public static final double  SCALE           = 10f; // 1 decimal places
    public static final double  MIN_TEMPERATURE = 0.0d;
    public static final double  MAX_TEMPERATURE =
            (Short.MAX_VALUE - (long) Short.MIN_VALUE) / SCALE; // ≈ 42949.67295
    private final       float[] values          = new float[SIZE];
    //private final int[] defaultData = new int[SIZE];

    @Override
    public @NotNull TemperatureDataLayer fromBytes(byte @NotNull [] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) values[i] = (float) ((buffer.getShort() - Short.MIN_VALUE) / SCALE);
        //for (int i = 0; i < SIZE; i++) defaultData[i] = buffer.getInt();
        return this;
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * 2);// * 2);
        for (float value : values) buffer.putShort((short) ((value * SCALE) + Short.MIN_VALUE));
        //for (int val : defaultData) buffer.putInt(val);
        return buffer.array();
    }

    @Override
    public float getDirect(short idx) {
        return values[idx];
    }

    @Override
    protected float decode(short index) {
        return 0;
    }

    @Override
    public void setDirect(short idx, float value) {
        values[idx] = value;
    }

    @Override
    protected void encode(short index, float value) {
    }
}