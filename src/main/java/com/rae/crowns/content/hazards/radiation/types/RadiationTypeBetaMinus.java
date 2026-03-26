package com.rae.crowns.content.hazards.radiation.types;

import com.rae.crowns.content.hazards.ItemRadiation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RadiationTypeBetaMinus extends RadiationTypeBase {
    @Override
    public void onUpdate(LivingEntity target, ItemRadiation.DecayContainer container, ItemStack stack) {
        // Electrons burn your skin and do the same thing as alphas but weaker, a bit spicier
    }
}
