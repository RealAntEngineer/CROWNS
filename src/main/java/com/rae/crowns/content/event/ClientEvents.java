package com.rae.crowns.content.event;

import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.sound.CrownsSoundScapes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!isGameActive())
            return;

        Level world = Minecraft.getInstance().level;

        CrownsSoundScapes.tick();
    }

    @SubscribeEvent
    public static void addToItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null)
            return;

        ItemStack itemStack = event.getItemStack();
        List<Component> components = event.getToolTip();
        CustomData nbt = itemStack.get(DataComponents.CUSTOM_DATA);
        if (nbt != null) {
            CompoundTag composition = (CompoundTag) nbt.copyTag().get("composition");
            if (composition != null) {
                components.add(Component.literal("composition").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
                for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
                    if (composition.contains(resourceLocation.toString())) {
                        float concentration = composition.getFloat(resourceLocation.toString());
                        components.add(
                                Component.translatable(resourceLocation.toLanguageKey("nucleus")).withStyle(ChatFormatting.YELLOW)
                                        .append(Component.literal(String.format(" : %e %%", concentration)).withStyle(ChatFormatting.GRAY)));
                    }
                }
            }
        }
    }
    protected static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }

}
