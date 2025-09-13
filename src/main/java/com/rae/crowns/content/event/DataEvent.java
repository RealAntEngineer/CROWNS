package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class DataEvent {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer)
            SteamFlowManager.playerLoaded(serverPlayer);

    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SteamFlowManager.serverStarted(event.getServer());
    }
}