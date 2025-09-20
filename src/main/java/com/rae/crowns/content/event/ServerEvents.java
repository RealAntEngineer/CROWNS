package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onServerLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel)) return;

        SteamFlowManager.tick(serverLevel);

    }

}
