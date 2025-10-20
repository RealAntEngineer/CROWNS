package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
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
            Set<SectionPos> dirtySections = new HashSet<>(data.getLoadedSections().size());
            for (SectionPos pos : data.getLoadedSections()) {
                if (serverLevel.isAreaLoaded(pos.origin(), 1) && data.isDirty(pos)) {
                    dirtySections.add(pos);
                }
            }
            TemperatureTicker.tick(dirtySections, data);
        }

        if (tickCounter % (20) == 0) {
            TemperatureManager.sendUpdate(serverLevel);
        }
        SteamFlowManager.tick(serverLevel);
        tickCounter++;
    }

}