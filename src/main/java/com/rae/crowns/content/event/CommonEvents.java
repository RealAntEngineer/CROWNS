package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.compressor.CompressorBlockEntity;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlockEntity;
import com.rae.crowns.init.data.AttachementTypeInit;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CROWNS.MODID)
public class CommonEvents {
    private static int tickCounter = 1;
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        tickCounter++;
        if (tickCounter % 40 == 0) {//lazy ticking
            //TemperatureTicker.tick(serverLevel);
        }
        if (tickCounter % 20 == 0) {
            TemperatureWorldData data = TemperatureManager.get(serverLevel);
            data.initialise(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        //System.out.println("hello ?");
        if (event.getLevel() instanceof ServerLevel level) {
            TemperatureWorldData worldData = TemperatureManager.get(level);

            if (event.getChunk().hasData(AttachementTypeInit.CHUNK_TEMPERATURE.get())){
                worldData.put(event.getChunk().getPos(), event.getChunk().getData(AttachementTypeInit.CHUNK_TEMPERATURE.get()));

            } else {
                worldData.putForInitialisation(event.getChunk().getPos());
            }

        }

    }


    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            SteamCollectorBlockEntity.registerCapabilities(event);
            SteamInputBlockEntity.registerCapabilities(event);
            CompressorBlockEntity.registerCapabilities(event);
            HeatExchangerBlockEntity.registerCapabilities(event);
        }

    }
}
