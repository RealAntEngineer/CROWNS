package com.rae.crowns.content.fields.temperature;

import net.minecraft.util.Mth;

import java.nio.ByteBuffer;

public class TemperatureDataLayer {
    private static final int SIZE = 16 * 16 * 16;
    private final short[] data;

    public TemperatureDataLayer() {
        this.data = new short[16 * 16 * 16]; // One short per block in a chunk section
    }
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * 2);
        for (short val : data) {
            buffer.putShort(val);
        }
        return buffer.array();
    }

    public static TemperatureDataLayer fromBytes(byte[] bytes) {
        TemperatureDataLayer temp = new TemperatureDataLayer();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) {
            temp.data[i] = buffer.getShort();
        }
        return temp;
    }

    public short[] getRaw() {
        return data;
    }

    public float get(int x, int y, int z) {
        return (float) (data[y << 8 | z << 4 | x] + 32768) / 10;
    }

    public void set(int x, int y, int z, float temperature) {//map
        data[y << 8 | z << 4 | x] = (short) (Mth.clamp(temperature,0,6553.5) * 10 - 32768);
    }
}