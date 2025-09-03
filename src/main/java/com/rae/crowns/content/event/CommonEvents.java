package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.compressor.CompressorBlockEntity;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlockEntity;
import com.rae.crowns.init.misc.CommandsInit;
import com.simibubi.create.infrastructure.command.AllCommands;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.stream.Collectors;

@EventBusSubscriber(modid = CROWNS.MODID)
public class CommonEvents {
    private static int tickCounter = 1;
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        if (tickCounter % 20 == 0) {
            TemperatureWorldData data = TemperatureManager.get(serverLevel);
            data.initialise(serverLevel);
        }
        if (tickCounter % (int)(20*TemperatureTicker.DT) == 0) {
            //lazy ticking
            TemperatureWorldData data = TemperatureManager.get(serverLevel);
            TemperatureTicker.tick(data.getLoadedSections().stream()
                    .filter(pos -> serverLevel.isAreaLoaded(pos.origin(),1) && data.isDirty(pos))
                    .collect(Collectors.toSet()), data);
        }
        tickCounter++;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandsInit.register(event.getDispatcher());
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
