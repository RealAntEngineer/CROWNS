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
        if (container.alpha > 0) {
            RadTypeAlpha.onUpdate(entity, container, stack);
        }
        if (container.beta_minus > 0) {
            RadTypeBetaMinus.onUpdate(entity, container, stack);
        }
        if (container.beta_plus > 0) {
            RadTypeBetaPlus.onUpdate(entity, container, stack);
        }
        if (container.sf > 0) {
            RadTypeFission.onUpdate(entity, container, stack);
        }
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