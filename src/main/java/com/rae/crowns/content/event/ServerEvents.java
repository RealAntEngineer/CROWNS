package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class ServerEvents {
    private static int tickCounter = 1;

    @SubscribeEvent
    public static void onServerLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel)) return;
        if (!event.haveTime()) return;
        TemperatureWorldData data = TemperatureManager.get(serverLevel);
        data.initialise(serverLevel);
        data.updateChangedBlocks(serverLevel);
        if (tickCounter % (TemperatureTicker.TICK_PERIOD) == 0) {
            //this is too long...
            LongSet loadedSections = data.getLoadedSections(); // LongSet view of keys
            LongSet nearDynamicSections = data.getNearDynamic();
            LongSet toTick = new LongOpenHashSet();

            // Compute intersection efficiently
            for (long packed : nearDynamicSections) {
                if (loadedSections.contains(packed)) {
                    if (data.isDirty(packed)) {
                        toTick.add(packed);
                    }
                }
            }

            TemperatureTicker.tick(toTick, data);

        }

        if (tickCounter % (20) == 0) {
            TemperatureManager.sendUpdate(serverLevel);
        }
        SteamFlowManager.tick(serverLevel);
        tickCounter++;
    }

}