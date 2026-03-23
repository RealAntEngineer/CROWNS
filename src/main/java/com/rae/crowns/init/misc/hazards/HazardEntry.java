package com.rae.crowns.init.misc.hazards;

import com.rae.crowns.init.misc.hazards.types.HazardTypeBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HazardEntry implements Cloneable {

    final HazardTypeBase type;
    final double baseLevel;

    public HazardEntry(final HazardTypeBase type){this(type, 1D);}

    public HazardEntry(final HazardTypeBase type, double level) {
        this.type = type;
        this.baseLevel = level;
    }

    public void applyHazard(final ItemStack stack, final LivingEntity entity) {
        type.onUpdate(entity, baseLevel, stack);
    }

    @Override
    public HazardEntry clone() {
        try {
            return (HazardEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public HazardEntry clone(double mult) {
        return new HazardEntry(type, baseLevel * mult);
    }
}

// Github desktop is retarded