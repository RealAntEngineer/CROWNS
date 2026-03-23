package com.rae.crowns.init.misc.hazards;

import com.rae.crowns.init.misc.hazards.types.HazardTypeBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mod.EventBusSubscriber
public class HazardSystem {

    public static final HashMap<Item, HazardData> itemMap = new HashMap<>();

    public static void register(Object o, HazardData data) {
        itemMap.put((Item)o, data);
    }

    public static List<HazardEntry> getHazardsFromStack(ItemStack stack) {
        List<HazardData> chronological = new ArrayList<>();

        if (itemMap.containsKey(stack.getItem()))
            chronological.add(itemMap.get(stack.getItem()));

        List<HazardEntry> entries = new ArrayList<>();
        int mutex = 0;

        for (HazardData data : chronological) {
            if (data.doesOverride)
                entries.clear();

            if ((data.getMutex() & mutex) == 0) {
                entries.addAll(data.entries);
                mutex |= data.getMutex();
            }
        }

        return entries;
    }

    public static double getHazardLevelFromStack(ItemStack stack, HazardTypeBase hazard) {
        List<HazardEntry> entries = getHazardsFromStack(stack);

        for(HazardEntry entry : entries) {
            if(entry.type == hazard) {
                return entry.baseLevel;
            }
        }

        return 0F;
    }

    public static void applyHazards(ItemStack stack, LivingEntity entity) {
        List<HazardEntry> hazards = getHazardsFromStack(stack);

        for(HazardEntry hazard : hazards) {
            hazard.applyHazard(stack, entity);
        }
    }

    public static void updatePlayerInventory(Player player) {

        for (int i = 0; i < player.getInventory().items.size(); i++) {

            ItemStack stack = player.getInventory().getItem(i);
            applyHazards(stack, player);

            if (stack.isEmpty()) {
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }

        // Radiation won't apply to offhand yet but i'll soon fix that
    }

    @OnlyIn(Dist.CLIENT)
    public static void addFullTooltip(ItemStack stack, Player player, List<String> list) {

        List<HazardEntry> hazards = getHazardsFromStack(stack);

        for(HazardEntry hazard : hazards) {
            hazard.type.addHazardInformation(player, list, hazard.baseLevel, stack);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            updatePlayerInventory(event.player);
        }
    }
}

// Github desktop is retarded
