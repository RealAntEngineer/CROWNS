package com.rae.crowns.init.misc;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.EnchantedGoldenAppleItem;
import net.minecraft.world.item.Item;

import static com.rae.crowns.CROWNS.REGISTRATE;

@SuppressWarnings("ALL")
public class ItemInit {

    //to do list -> uranium ore (enrichment ?) + plutonium (created from 235) + depletion of fuel


    public static final ItemEntry<Item>
            URANIUM_INGOT = REGISTRATE.item("uranium_ingot", Item::new).register(),
            FUEL_ROD = REGISTRATE.item("fuel_rod", Item::new).register(),
            DEPLETED_URANIUM_INGOT = REGISTRATE.item("depleted_uranium_ingot", Item::new).register(),
            ENRICHED_URANIUM_NUGGET = REGISTRATE.item("enriched_uranium_nugget", Item::new).register(),
            DEPLETED_URANIUM_NUGGET = REGISTRATE.item("depleted_uranium_nugget", Item::new).register(),
            RAW_URANIUM = REGISTRATE.item("raw_uranium", Item::new).register();

    public static final ItemEntry<EnchantedGoldenAppleItem> ENRICHED_URANIUM_INGOT = REGISTRATE.item("enriched_uranium_ingot", EnchantedGoldenAppleItem::new).register();

    public static void register() {
    }

}
