package com.rae.crowns.content.fields.temperature;

import net.minecraft.util.Mth;

import java.nio.ByteBuffer;

/**
 * implement conduction coef for a Section (16, 16, 16)
 * conduction is coded on a byte from 0 to 655.36 (it's the byte squared, yes it's confusing for the datapack)
 */
public class ConductionDataLayer {
    public static final int SIZE = 16 * 16 * 16;
    private final byte[] data;


    public ConductionDataLayer() {
        this.data = new byte[16 * 16 * 16];
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
        for (byte val : data) {
            buffer.put(val);
        }
        return buffer.array();
    }

    public float get(int x, int y, int z) {
        int temp = data[y << 8 | z << 4 | x] + 128;
        return temp * temp / 100f;
    }


    public void set(int x, int y, int z, float conduction) {//map
        data[y << 8 | z << 4 | x] = (byte) (Mth.clamp(Math.sqrt(Math.abs(conduction * 100)), 0, 255) - 128);
    }
}