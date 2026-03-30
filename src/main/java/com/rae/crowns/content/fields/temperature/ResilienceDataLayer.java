package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.fields.util.AbstractDataLayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ResilienceDataLayer extends AbstractDataLayer {
    public static final int    SIZE = 16 * 16 * 16;
    private final       byte[] data = new byte[SIZE];

    @Override
    public @NotNull ResilienceDataLayer fromBytes(byte @NotNull [] bytes) {
        System.arraycopy(bytes, 0, data, 0, Math.min(bytes.length, SIZE));
        return this;
    }

    @Override
    public byte[] toBytes() {
        return data.clone();
    }

    @Override
    protected float decode(int index) {
        return (data[index] + 128) / 255f;
    }

    @Override
    protected void encode(int index, float value) {
        int scaled = Math.round(Mth.clamp(value, 0f, 1f) * 255f);
        data[index] = (byte) (scaled - 128);
    }
}