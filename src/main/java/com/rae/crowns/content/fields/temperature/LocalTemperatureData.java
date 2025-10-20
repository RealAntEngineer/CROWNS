package com.rae.crowns.content.fields.temperature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class LocalTemperatureData {
    private static final Map<SectionPos, TemperatureDataLayer> temperatureMap = new HashMap<>();
    private static ResourceLocation location = null;


    public static void receiveFullUpdate(Map<SectionPos, TemperatureDataLayer> serverData, ResourceLocation location) {
        LocalTemperatureData.location = location;
        temperatureMap.clear();
        temperatureMap.putAll(serverData);
    }

    public static void receiveUpdate(Map<SectionPos, TemperatureDataLayer> serverData) {
        temperatureMap.putAll(serverData);
    }

    public static float getTemperature(Vec3i pos) {
        SectionPos sectionPos = SectionPos.of((BlockPos) pos);
        TemperatureDataLayer layer = temperatureMap.get(sectionPos);

        if (layer == null) return 300;

        // Convert world coordinates to local (0–15) section coordinates
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        return layer.get(localX, localY, localZ);
    }
}
