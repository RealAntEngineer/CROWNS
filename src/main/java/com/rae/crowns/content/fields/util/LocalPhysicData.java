package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.advection.VelocityDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class LocalPhysicData {
    private static final Map<SectionPos, TemperatureDataLayer> temperatureMap = new HashMap<>();
    //private static final Map<SectionPos, VelocityDataLayer> vxMap = new HashMap<>();
    //private static final Map<SectionPos, VelocityDataLayer> vyMap = new HashMap<>();
    //private static final Map<SectionPos, VelocityDataLayer> vzMap = new HashMap<>();
    private static final Set<SectionPos> tickingSections = new HashSet<>();
    private static final Map<SectionPos, Long> lastTicked = new HashMap<>();
    private static final long MAX_TICKS_AGE = 5; // keep highlighting for 5 ticks
    private static @Nullable ResourceLocation location = null;

    public static void receiveFullUpdate(@NotNull Map<SectionPos, TemperatureDataLayer> serverData, ResourceLocation location) {
        LocalPhysicData.location = location;
        temperatureMap.clear();
        temperatureMap.putAll(serverData);
    }

    public static void receiveUpdate(@NotNull Map<SectionPos, TemperatureDataLayer> serverTemperatureData,
                                     /*@NotNull Map<SectionPos, VelocityDataLayer> vxData,
                                     @NotNull Map<SectionPos, VelocityDataLayer> vyData,
                                     @NotNull Map<SectionPos, VelocityDataLayer> vzData,*/ long currentTick) {
        temperatureMap.putAll(serverTemperatureData);
        //vxMap.putAll(vxData);
        //vyMap.putAll(vyData);
        //vzMap.putAll(vzData);

        // Mark all sections in this batch as ticking in this tick
        for (SectionPos section : serverTemperatureData.keySet()) {
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

    /*public static Vec3 getV(@NotNull Vec3i pos) {
        SectionPos sectionPos = SectionPos.of((BlockPos) pos);
        VelocityDataLayer vx = vxMap.get(sectionPos);
        VelocityDataLayer vy = vyMap.get(sectionPos);
        VelocityDataLayer vz = vzMap.get(sectionPos);

        if (vx == null || vy == null || vz == null) return Vec3.ZERO;

        // Convert world coordinates to local (0–15) section coordinates
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        return new Vec3(vx.get(localX, localY, localZ),vy.get(localX, localY, localZ),vz.get(localX, localY, localZ));
    }*/

    public static @NotNull Set<SectionPos> getTickingSections(){
        return tickingSections;
    }
}
