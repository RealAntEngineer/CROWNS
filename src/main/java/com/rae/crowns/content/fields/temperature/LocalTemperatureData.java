package com.rae.crowns.content.fields.temperature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class LocalTemperatureData {
    private static final Map<SectionPos, TemperatureDataLayer> temperatureMap = new HashMap<>();
    private static final Set<SectionPos> tickingSections = new HashSet<>();
    private static final Map<SectionPos, Long> lastTicked = new HashMap<>();
    private static final long MAX_TICKS_AGE = 5; // keep highlighting for 5 ticks
    private static @Nullable ResourceLocation location = null;

    public static void receiveFullUpdate(@NotNull Map<SectionPos, TemperatureDataLayer> serverData, ResourceLocation location) {
        LocalTemperatureData.location = location;
        temperatureMap.clear();
        temperatureMap.putAll(serverData);
    }

    public static void receiveUpdate(@NotNull Map<SectionPos, TemperatureDataLayer> serverData, long currentTick) {
        temperatureMap.putAll(serverData);

        // Mark all sections in this batch as ticking in this tick
        for (SectionPos section : serverData.keySet()) {
            tickingSections.add(section);
            lastTicked.put(section, currentTick);
        }

        // Prune old sections
        Iterator<SectionPos> it = tickingSections.iterator();
        while (it.hasNext()) {
            SectionPos section = it.next();
            long tick = lastTicked.getOrDefault(section, currentTick);
            if (currentTick - tick > MAX_TICKS_AGE) {
                it.remove();
                lastTicked.remove(section);
            }
        }
    }

    public static float getTemperature(@NotNull Vec3i pos) {
        SectionPos sectionPos = SectionPos.of((BlockPos) pos);
        TemperatureDataLayer layer = temperatureMap.get(sectionPos);

        if (layer == null) return 300;

        // Convert world coordinates to local (0–15) section coordinates
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        return layer.get(localX, localY, localZ);
    }

    public static @NotNull Set<SectionPos> getTickingSections(){
        return tickingSections;
    }
}
