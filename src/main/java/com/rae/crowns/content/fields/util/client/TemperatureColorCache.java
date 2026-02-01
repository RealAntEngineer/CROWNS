package com.rae.crowns.content.fields.util.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class TemperatureColorCache {
    private static final Vec3 ONE = new Vec3(1,1,1);
    private static Vec3 cachedTint = ONE;
    private static boolean dirty = true;

    private TemperatureColorCache() {}

    /**
     * Called when new temperature data arrives from the server.
     * Hook this from LocalPhysicData.receiveUpdate(...)
     */
    public static void markDirty() {
        dirty = true;
    }

    /**
     * Returns a normalized (0–1) RGB tint.
     * Safe to call from ModelBlockRenderer.
     */
    public static Vec3 getCurrentTint() {
        if (!dirty) {
            return cachedTint;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cachedTint = ONE;
            dirty = false;
            return cachedTint;
        }

        Set<SectionPos> ticking = LocalPhysicData.getTickingSections();
        if (ticking.isEmpty()) {
            cachedTint = ONE;
            dirty = false;
            return cachedTint;
        }

        double sum = 0.0;
        int count = 0;

        // Sample one representative point per section (center)
        for (SectionPos section : ticking) {
            BlockPos center = section.center();
            float temp = LocalPhysicData.getTemperature(center);
            sum += temp;
            count++;
        }

        float avgTemp = (float) (sum / count);

        cachedTint = temperatureToVec(avgTemp);
        dirty = false;
        return cachedTint;
    }

    /**
     * Converts temperature (K) → normalized RGB (0–1)
     * Matches your DebugRenderer logic.
     */
    private static Vec3 temperatureToVec(float temperature) {
        float t = Mth.clamp((temperature - 200f) / 200f, 0f, 1f);

        float r = t;
        float g = 1f - Math.abs(t - 0.5f) * 2f;
        float b = 1f - t;

        return new Vec3(
                Mth.clamp(r, 0f, 1f),
                Mth.clamp(g, 0f, 1f),
                Mth.clamp(b, 0f, 1f)
        );
    }
}

