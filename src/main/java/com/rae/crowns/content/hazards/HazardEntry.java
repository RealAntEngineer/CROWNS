package com.rae.crowns.content.hazards;

import com.rae.crowns.content.hazards.radiation.types.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
public class HazardEntry implements Cloneable {
    ItemRadiation.DecayContainer container;
    // Oh lord
    RadiationTypeElectromagnetic RadTypeEM = new RadiationTypeElectromagnetic();
    RadiationTypeBetaPlus RadTypeBetaPlus = new RadiationTypeBetaPlus();
    RadiationTypeBetaMinus RadTypeBetaMinus = new RadiationTypeBetaMinus();
    RadiationTypeFission RadTypeFission = new RadiationTypeFission();
    RadiationTypeAlpha RadTypeAlpha = new RadiationTypeAlpha();

    public HazardEntry(final ItemRadiation.DecayContainer type) {
        this.container = type;
    }

    public void applyHazard(ItemStack stack, LivingEntity entity) {
        RadTypeAlpha.onUpdate(entity, container, stack);
        RadTypeBetaPlus.onUpdate(entity, container, stack);
        RadTypeBetaMinus.onUpdate(entity, container, stack);
        RadTypeFission.onUpdate(entity, container, stack);
        RadTypeEM.onUpdate(entity, container, stack);
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