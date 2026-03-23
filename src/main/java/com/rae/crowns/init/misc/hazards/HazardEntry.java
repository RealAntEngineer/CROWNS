package com.rae.crowns.init.misc.hazards;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HazardEntry implements Cloneable {
    final ItemRadiation.DecayContainer container;

    public HazardEntry(final ItemRadiation.DecayContainer type) {
        this.container = type;
    }

    @Override
    public HazardEntry clone() {
        try {
            return (HazardEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

// Github desktop is retarded