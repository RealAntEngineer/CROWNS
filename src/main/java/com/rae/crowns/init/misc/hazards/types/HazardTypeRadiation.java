package com.rae.crowns.init.misc.hazards.types;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

public class HazardTypeRadiation extends HazardTypeBase {

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        // This is just for the tooltip really
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(Player player, List<String> list, double level, ItemStack stack) {
        list.add("§a[Radioactive]");
        list.add(" §e-:: " + String.format("%.2f", getNewValue(level)) + getSuffix(level) + "Bq"); // Floating point numbers are fucking evil

        if (stack.getCount() > 1) {
            double stackRad = level * stack.getCount();
            list.add(" §e-:: Stack:"+ String.format("%.2f", getNewValue(stackRad)) + getSuffix(stackRad) + "Bq"); // Again
        }
    }

    public static double getNewValue(double radiation) {
        if (radiation < 1000000) {
            return radiation;
        } else if (radiation < 1000000000) {
            return radiation * 0.000001D;
        } else {
            return radiation * 0.000000001D;
        }
    }

    public static String getSuffix(double radiation) {
        if (radiation < 1000000) {
            return "";
        } else if (radiation < 1000000000) {
            return "M";
        } else {
            return "G";
        }
    }
}

// Github desktop is retarded