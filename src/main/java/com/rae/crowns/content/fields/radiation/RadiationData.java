package com.rae.crowns.content.fields.radiation;

public class RadiationData {
    private final short[] data;

    public RadiationData() {
        this.data = new short[16 * 16 * 16]; // One short per block in a chunk section
    }

    public short getRadiation(int x, int y, int z) {
        int index = (y * 16 + z) * 16 + x;
        return data[index];
    }

    public void setRadiation(int x, int y, int z, short radiation) {
        int index = (y * 16 + z) * 16 + x;
        data[index] = radiation;
    }
}