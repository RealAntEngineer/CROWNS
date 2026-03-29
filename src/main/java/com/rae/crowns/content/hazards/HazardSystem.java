package com.rae.crowns.content.hazards;

import com.rae.formicapi.FormicApiLang;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber
public class HazardSystem {

    public static final HashMap<Item, HazardData> itemMap = new HashMap<>();

    public static void register(Object o, HazardData data) {
        if (o instanceof Item)
            itemMap.put((Item)o, data);
        if (o instanceof Block)
            itemMap.put(((Block) o).asItem(), data);
    }

    public static List<HazardEntry> getEntriesFromStack(ItemStack stack) {
        List<HazardData> chronological = new ArrayList<>();

        if (itemMap.containsKey(stack.getItem()))
            chronological.add(itemMap.get(stack.getItem()));

        List<HazardEntry> entries = new ArrayList<>();
        int mutex = 0;

        for (HazardData data : chronological) {
            if (data.doesOverride)
                entries.clear();

            if ((data.getMutex() & mutex) == 0) {
                entries.add(data.entry);
                mutex |= data.getMutex();
            }
        }

        return entries;
    }

    public static void applyHazards(ItemStack stack, LivingEntity entity) {
        List<HazardEntry> entries = getEntriesFromStack(stack);

        for (HazardEntry entry : entries) {
            entry.applyHazard(stack, entity);
        }
    }

    public static void updatePlayerInventory(Player player) {

        for (int i = 0; i < player.getInventory().items.size(); i++) {

            ItemStack stack = player.getInventory().getItem(i);
            // Implementation of radiation will go here
            applyHazards(stack, player);

            if (stack.isEmpty()) {
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }

        // Radiation won't apply to offhand yet but i'll soon fix that
    }

    public static Optional<HazardEntry> getOptionalEntryFromItem(Item item) {
        return Optional.ofNullable(itemMap.get(item)).map(data -> data.entry);
    }

    public static Optional<HazardEntry> getOptionalEntryFromBlock(Block block) {
        return getOptionalEntryFromItem(block.asItem());
    }

    @OnlyIn(Dist.CLIENT)
    public static void addFullTooltip(ItemStack stack, Player player, List<String> list) {

        List<HazardEntry> entries = getEntriesFromStack(stack);

        for(HazardEntry entry : entries) {
            list.add("§a[Radioactive]");
            list.add(" §e" + FormicApiLang.numberWithSymbol(entry.container.specific_activity).component().getString() + "Bq");

            if (entry.container.sf != 0) {
                list.add("  §4-:: Spontaneous fission: " + entry.container.sf * 100 + "%");
            }

            if (entry.container.beta_plus != 0) {
                list.add("  §b-:: Beta⁺ decay channel: " + entry.container.beta_plus * 100 + "%");
            }

            if (entry.container.beta_minus != 0) {
                list.add("  §b-:: Beta⁻ decay channel: " + entry.container.beta_minus * 100 + "%");
            }

            if (entry.container.alpha != 0) {
                list.add("  §c-:: Alpha decay channel: " + entry.container.alpha * 100 + "%");
            }

            if (entry.container.branching_ratio != 0) {
                list.add("");
                list.add(" §dPrompt gammas: " + FormicApiLang.numberWithSymbol(entry.container.getReontgen(0.1D)).component().getString() + "R/s");
            }

            if (stack.getCount() > 1) {
                list.add("");
                list.add(" §eStack: " + FormicApiLang.numberWithSymbol(entry.container.specific_activity*stack.getCount()).component().getString() + "Bq");
                list.add(" §dStack prompt gammas: " + FormicApiLang.numberWithSymbol(entry.container.multiply(stack.getCount()).getReontgen(0.01D)).component().getString() + "R/s");
            }
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
