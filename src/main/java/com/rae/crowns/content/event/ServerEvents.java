package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class ServerEvents {
    private static int tickCounter = 1;
    @SubscribeEvent
    public static void onServerLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel)) return;
        TemperatureWorldData data = TemperatureManager.get(serverLevel);
        data.initialise(serverLevel);
        data.updateChangedBlocks(serverLevel);
        if (tickCounter % (TemperatureTicker.TICK_PERIOD) == 0) {
            //lazy ticking
            TemperatureTicker.tick(data.getLoadedSections().stream()
                    .filter(pos -> serverLevel.isAreaLoaded(pos.origin(),1) && data.isDirty(pos))
                    .collect(Collectors.toSet()), data);
        }

        if (tickCounter % (20) == 0) {
            TemperatureManager.sendUpdate(serverLevel);
        }
        tickCounter++;
    }

}