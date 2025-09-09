package com.rae.crowns.content.fields.temperature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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

    public static void receiveUpdate(Map<SectionPos, TemperatureDataLayer> serverData){
        temperatureMap.putAll(serverData);
    }

    public static float getTemperature(Vec3i pos) {
        if (!temperatureMap.containsKey(SectionPos.of((BlockPos) pos))) return 300;
        return temperatureMap.get(SectionPos.of((BlockPos) pos)).get(pos.getX(), pos.getY(), pos.getZ());
    }
}
