package com.rae.crowns.init.misc.hazards.types;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

public abstract class HazardTypeBase {
    public abstract void onUpdate(LivingEntity target, double level, ItemStack stack);

    @OnlyIn(Dist.CLIENT)
    public abstract void addHazardInformation(Player player, List<String> list, double level, ItemStack stack);
}

// Github desktop is retarded