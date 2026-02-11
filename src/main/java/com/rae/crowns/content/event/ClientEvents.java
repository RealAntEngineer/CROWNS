package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.sound.CrownsSoundScapes;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import static net.createmod.ponder.PonderClient.isGameActive;

@EventBusSubscriber(modid = CROWNS.MODID)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel().isClientSide)) return;

        Level world = Minecraft.getInstance().level;

        CrownsSoundScapes.tick();
        SteamFlowManager.tick(event.getLevel());

    }
}
