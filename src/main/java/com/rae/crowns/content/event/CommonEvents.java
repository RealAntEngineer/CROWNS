package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class CommonEvents {
    private static int tickCounter = 1;
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel)) return;
        TemperatureWorldData data = TemperatureManager.get(serverLevel);
        data.initialise(serverLevel);
        if (tickCounter % (20*TemperatureTicker.DT) == 0) {
            //lazy ticking
            TemperatureTicker.tick(data.getLoadedSections().stream()
                    .filter(pos -> serverLevel.isAreaLoaded(pos.origin(),1) && data.isDirty(pos))
                    .collect(Collectors.toSet()), data);
        }
        tickCounter++;
    }

}