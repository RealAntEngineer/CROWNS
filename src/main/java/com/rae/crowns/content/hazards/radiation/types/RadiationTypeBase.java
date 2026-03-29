package com.rae.crowns.content.hazards.radiation.types;

import com.rae.crowns.content.hazards.radiation.ItemRadiation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public abstract class RadiationTypeBase {
    /**
     * Does the stuff and applies RADs
     * @param target the holder
     * @param container the final level after calculating all the modifiers
     * @param stack stack that is being updated
     */
    public abstract void onUpdate(LivingEntity target, ItemRadiation.DecayContainer container, ItemStack stack);
}
