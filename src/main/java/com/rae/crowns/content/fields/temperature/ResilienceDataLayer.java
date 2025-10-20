package com.rae.crowns.content.fields.temperature;

import net.minecraft.util.Mth;

import java.nio.ByteBuffer;

/**
 * implement resilience to  for a Section (16, 16, 16)
 * temperature is coded on a byte from 0 to 255 with a step of 1/255
 */
public class ResilienceDataLayer {
    public static final int SIZE = 16 * 16 * 16;
    private final byte[] data;


    public ResilienceDataLayer() {
        this.data = new byte[16 * 16 * 16];
    }

    public static ResilienceDataLayer fromBytes(byte[] bytes) {
        ResilienceDataLayer temp = new ResilienceDataLayer();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) {
            temp.data[i] = buffer.get();
        }
        return temp;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        for (byte val : data) {
            buffer.put(val);
        }
        return buffer.array();
    }

    public byte[] getRaw() {
        return data;
    }

    public float get(int x, int y, int z) {
        return (data[y << 8 | z << 4 | x] + 128f) / 255f;
    }


    public void set(int x, int y, int z, float resilience) {//map
        data[y << 8 | z << 4 | x] = (byte) (Mth.clamp(resilience, 0, 1) * 255f - 128);
    }
}