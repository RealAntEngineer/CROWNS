package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class CommonEvents {
    private static int tickCounter = 1;
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        if (tickCounter % 20 == 0) {
            TemperatureWorldData data = TemperatureManager.get(serverLevel);
            data.initialise(serverLevel);
        }
        if (tickCounter % (int)(20) == 0) {
            //lazy ticking
            TemperatureWorldData data = TemperatureManager.get(serverLevel);
            TemperatureTicker.tick(data.getLoadedSections().stream()
                    .filter(pos -> serverLevel.isAreaLoaded(pos.origin(),1) && data.isDirty(pos))
                    .collect(Collectors.toSet()), data);
        }
        tickCounter++;
    }
}
