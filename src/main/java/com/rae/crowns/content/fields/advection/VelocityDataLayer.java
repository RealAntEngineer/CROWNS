package com.rae.crowns.content.fields.advection;

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
public class VelocityDataLayer extends AbstractDataLayer {
    public static final double SCALE = 100f; // 5 decimal places
    public static final double MIN_SPEED = Short.MIN_VALUE/SCALE;
    public static final double MAX_SPEED = Short.MAX_VALUE/SCALE; // ≈ 42949.67295
    private final short[] data = new short[SIZE];

    @Override
    public VelocityDataLayer fromBytes(byte @NotNull [] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) data[i] = buffer.getShort();
        //for (int i = 0; i < SIZE; i++) defaultData[i] = buffer.getInt();
        return this;
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * 2);// * 2);
        for (short val : data) buffer.putShort(val);
        //for (int val : defaultData) buffer.putInt(val);
        return buffer.array();
    }

    @Override
    protected float decode(int index) {
        int stored = data[index];
        return (float) (stored / SCALE);
    }

    @Override
    protected void encode(int index, float value) {
        short encoded = (short) (value * SCALE);
        data[index] = encoded;
    }
}