package com.rae.crowns.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeModeTabsInit {

    public static final CreativeModeTab NUCLEAR_TAB =
            new CreativeModeTab("crowns.nuclear") {
        @Override
        public ItemStack makeIcon() {
            return BlockItem.byBlock(BlockInit.FUEL_ASSEMBLY.get()).getDefaultInstance();
        }
    };
    public static void init() {
    }
}
