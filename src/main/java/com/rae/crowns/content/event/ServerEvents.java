package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

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
            Set<Long> dirtySections = new HashSet<>(data.getLoadedSections().size());
            for (long packed : data.getLoadedSections()) {
                int sx = (int) (packed >>> 40);
                int sy = (int) ((packed >>> 20) & 0xFFFFF);
                int sz = (int) (packed & 0xFFFFF);

                // section origin in block coordinates
                BlockPos origin = new BlockPos(sx << 4, sy << 4, sz << 4);

                if (serverLevel.isAreaLoaded(origin, 1) && data.isDirty(packed)) {
                    dirtySections.add(packed);
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