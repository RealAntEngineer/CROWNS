package com.rae.crowns.content.event;

import com.rae.crowns.content.thermodynamics.compressor.CompressorBlockEntity;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlockEntity;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


public class CommonEvents {
    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @net.neoforged.bus.api.SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            SteamCollectorBlockEntity.registerCapabilities(event);
            SteamInputBlockEntity.registerCapabilities(event);
            CompressorBlockEntity.registerCapabilities(event);
            HeatExchangerBlockEntity.registerCapabilities(event);
        }

    }
}
