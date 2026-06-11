package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.AbstractDataLayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * Conduction coefficient for a Section (16×16×16)
 * Stored as 8-bit mini-float: 2-bit mantissa + 6-bit signed exponent (-16 → +47)
 * Uses bit-shifts instead of Math.pow for speed.
 */
public class ConductionDataLayer extends AbstractDataLayer {
    public static final float  MIN_VALUE = 1.0f / (1 << 16);      // 2^-16
    public static final float  MAX_VALUE = 1.75f * (1L << 47);    // 1.75 * 2^47
    private final       byte[] data      = new byte[SIZE];
    private final       float[] values   = new float[SIZE];

    @Override
    public @NotNull ConductionDataLayer fromBytes(byte @NotNull [] bytes) {
        System.arraycopy(bytes, 0, data, 0, Math.min(bytes.length, SIZE));
        for (short i = 0; i < SIZE ; i++){
            values[i] = decode(i);
        }
        return this;
    }

    @Override
    public byte[] toBytes() {
        return data.clone();
    }

    @Override
    public float get(short x, short y, short z) {
        return values[index(x, y, z)];
    }

    @Override
    public float getDirect(short idx) {
        return values[idx];
    }
    @Override
    public void setDirect(short idx, float value) {
        encode(idx, value);
        values[idx] = value;
    }

    @Override
    protected float decode(short index) {
        int   b        = data[index] & 0xFF;
        int   mantissa = b & 0b11;//last 2 bits
        int   exponent = ((b >> 2) & 0b111111) - 16;//first 6 bit
        float m        = 1.0f + mantissa / 4.0f;
        return exponent >= 0 ? m * (1L << exponent) : m / (1L << -exponent);
    }

    @Override
    protected void encode(short index, float value) {
        float clamped  = Mth.clamp(value, MIN_VALUE, MAX_VALUE);
        int   exponent = (int) Math.floor(Math.log(clamped) / Math.log(2));
        exponent = Mth.clamp(exponent, -16, 47);
        float normalized = exponent >= 0 ? clamped / (1L << exponent) : clamped * (1L << -exponent);
        int   mantissa   = Mth.clamp(Math.round((normalized - 1f) * 4f), 0, 3);
        int   stored     = ((exponent + 16) << 2) | mantissa;
        data[index] = (byte) stored;
    }
}
