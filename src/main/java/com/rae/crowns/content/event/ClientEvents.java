package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CROWNS.MODID)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel().isClientSide)) return;

        SteamFlowManager.tick(event.getLevel());

    }
}
