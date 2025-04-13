package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = CROWNS.MODID)
public class DataEvents {

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event){
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TemperatureManager.get(serverLevel);
        }
    }
}
