package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;

import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CROWNS.MODID)
public class DataEvents {
    //put this inside the Physics world data, that way we will have access to the private maps


    @SubscribeEvent
    public static void onPlayerJoin(@NotNull PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer)
            SteamFlowManager.playerLoaded(serverPlayer);

    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PhysicsSaveManager.reset();//this in important to clean the data after leaving.
    }
    @SubscribeEvent
    public static void onServerStarted(@NotNull ServerStartedEvent event) {
        SteamFlowManager.serverStarted(event.getServer());
        PhysicsSaveManager.serverStarted(event.getServer());
    }
}
