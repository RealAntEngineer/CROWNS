package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class DataEvents {

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event){
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TemperatureManager.get(serverLevel);//here to hook into the debug mode (break point)
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event){
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TemperatureWorldData worldData = TemperatureManager.get(serverLevel);
            if (event.isNewChunk()){
                ChunkAccess chunk = event.getChunk();
                ChunkPos chunkPos = chunk.getPos();
                for (int i = chunk.getMinSection(); i  < chunk.getMaxSection(); i++){
                    worldData.putForInitialisation(SectionPos.of(chunkPos, i));
                }
            }
        }

    }
}
