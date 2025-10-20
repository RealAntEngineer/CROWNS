package com.rae.crowns.content.event;

import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber()
public class PlacementHandler {
    //redundant because we already do it with mixin
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        TemperatureWorldData tempData = TemperatureManager.get(level);

        tempData.set(pos, TemperatureManager.getDefaultTemperature(level,pos),TemperatureManager.getDefaultConduction(level,pos),
                TemperatureManager.getDefaultResilience(level,pos));
        tempData.setDirty(SectionPos.of(pos));
    }
}
