package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.util.PhysicThread;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class ServerEvents {
    //todo wrong place to put this
    //public static HashMap<AssemblyBlockEntity, BlockPos> assemblies = new HashMap<>();


    @SubscribeEvent
    public static void onPlayerJoin(@NotNull PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer)
            SteamFlowManager.playerLoaded(serverPlayer);

    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PhysicThread.shutdown();
        PhysicsSaveManager.reset();//this in important to clean the data after leaving.
    }

    @SubscribeEvent
    public static void onServerStarted(@NotNull ServerStartedEvent event) {
        SteamFlowManager.serverStarted(event.getServer());
        PhysicsSaveManager.serverStarted(event.getServer());
        PhysicThread.launchPhysicThread((double) 1 /TemperatureTicker.TICK_PERIOD);
    }
}