package com.rae.crowns.init;

import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;

import static com.rae.crowns.CROWNS.REGISTRATE;

public class ItemInit {

    //to do list -> uranium ore (enrichment ?) + plutonium (created from 235) + depletion of fuel


    public static final ItemEntry<Item> URANIUM_INGOT = REGISTRATE.item("uranium_ingot", Item::new)
            .properties(properties -> properties.tab(CreativeModeTabsInit.NUCLEAR_TAB))
            .register();
    public static void register() {}

}
