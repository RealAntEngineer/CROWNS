package com.rae.crowns.content.fields.advection;

import com.rae.crowns.content.fields.util.AbstractDataLayer;
import org.jetbrains.annotations.NotNull;


/**
 * Compact solid-face mask.
 * Each bit encodes one voxel: 1 = blocked, 0 = open.
 * 4096 voxels per section -> 512 bytes total.
 */
public class BlockedDataLayer extends AbstractDataLayer {
    private static final int BYTES = SIZE / 8; // 4096 / 8 = 512
    private final byte[] data = new byte[BYTES];

    @Override
    public @NotNull BlockedDataLayer fromBytes(byte @NotNull [] bytes) {
        int len = Math.min(bytes.length, BYTES);
        System.arraycopy(bytes, 0, data, 0, len);
        return this;
    }

    @Override
    public byte[] toBytes() {
        byte[] copy = new byte[BYTES];
        System.arraycopy(data, 0, copy, 0, BYTES);
        return copy;
    }

    /**
     * Returns 1 if blocked, 0 if open
     */
    @Override
    protected float decode(int index) {
        int byteIndex = index >> 3;       // index / 8
        int bitIndex = index & 7;         // index % 8
        boolean blocked = (data[byteIndex] & (1 << bitIndex)) != 0;
        return blocked ? 1f : 0f;
    }

    /**
     * Sets blocked if value >= 0.5
     */
    @Override
    protected void encode(int index, float value) {
        int byteIndex = index >> 3;
        int bitIndex = index & 7;
        if (value >= 0.5f) {
            data[byteIndex] |= (byte) (1 << bitIndex);   // set bit
        } else {
            data[byteIndex] &= (byte) ~(1 << bitIndex);  // clear bit
        }
    }

    public boolean isBlocked(int x, int y, int z) {
        int index = index(x, y, z);
        int byteIndex = index >> 3;
        int bitIndex = index & 7;
        return (data[byteIndex] & (1 << bitIndex)) != 0;
    }

    public void setBlocked(int x, int y, int z, boolean blocked) {
        int index = index(x, y, z);
        int byteIndex = index >> 3;
        int bitIndex = index & 7;
        if (blocked) {
            data[byteIndex] |= (byte) (1 << bitIndex);
        } else {
            data[byteIndex] &= (byte) ~(1 << bitIndex);
        }
    }
}