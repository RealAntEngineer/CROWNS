package com.rae.crowns.content.fields.temperature;

import net.minecraft.util.Mth;

import java.nio.ByteBuffer;

/**
 * implement temperature for a Section (16, 16, 16)
 * temperature is coded on a short from 0 to 6553.5 with a step of 0.1
 */
public class TemperatureDataLayer {
    public static final int SIZE = 16 * 16 * 16;
    public static final int MIN_TEMPERATURE = 0;
    public static final int MAX_TEMPERATURE = 6553;
    private static final int SHORT_SIZE = 256 * 256;
    private final short[] data;
    private final short[] defaultData;


    public TemperatureDataLayer() {
        this.data = new short[16 * 16 * 16];
        this.defaultData = new short[16 * 16 * 16];// One short per block in a chunk section
    }

    public static TemperatureDataLayer fromBytes(byte[] bytes) {
        TemperatureDataLayer temp = new TemperatureDataLayer();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < SIZE; i++) {
            temp.data[i] = buffer.getShort();
        }
        for (int i = 0; i < SIZE; i++) {
            temp.defaultData[i] = buffer.getShort();
        }
        return temp;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE * 4);
        for (short val : data) {
            buffer.putShort(val);
        }
        for (short val : defaultData) {
            buffer.putShort(val);
        }
        return buffer.array();
    }

    public short[] getRaw() {
        return data;
    }

    public float get(int x, int y, int z) {
        return (float) (data[y << 8 | z << 4 | x] + SHORT_SIZE / 2) / 10;
    }

    public float getDefault(int x, int y, int z) {
        return (float) (defaultData[y << 8 | z << 4 | x] + SHORT_SIZE / 2) / 10;
    }


    public void set(int x, int y, int z, float temperature) {//map
        data[y << 8 | z << 4 | x] = (short) ((int) Mth.clamp(temperature, MIN_TEMPERATURE, MAX_TEMPERATURE) * 10 - SHORT_SIZE / 2);
    }

    public void setDefault(int x, int y, int z, float temperature) {//map
        defaultData[y << 8 | z << 4 | x] = (short) ((int) Mth.clamp(temperature, MIN_TEMPERATURE, MAX_TEMPERATURE) * 10 - SHORT_SIZE / 2);
    }
}