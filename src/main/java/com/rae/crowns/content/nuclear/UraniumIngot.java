package com.rae.crowns.content.nuclear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class UraniumIngot extends Item {
    public UraniumIngot(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack defaultInstance = super.getDefaultInstance();
        CompoundTag nbt = defaultInstance.getOrCreateTag();
        CompoundTag composition = new CompoundTag();
        float refinement = 0.007f;
        composition.putFloat("crowns:u238", 1 - refinement);
        composition.putFloat("crowns:u235", refinement);
        nbt.put("composition", composition);
        defaultInstance.setTag(nbt);
        return defaultInstance;
    }

}
