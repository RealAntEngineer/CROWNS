package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.sound.CrownsSoundScapes;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.createmod.ponder.PonderClient.isGameActive;

@EventBusSubscriber(modid = CROWNS.MODID)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel().isClientSide)) return;

        Level world = Minecraft.getInstance().level;

        CrownsSoundScapes.tick();
        SteamFlowManager.tick(event.getLevel());

    }

    @SubscribeEvent
    public static void addToItemTooltip(@NotNull ItemTooltipEvent event) {
        if (event.getEntity() == null)
            return;

        ItemStack itemStack = event.getItemStack();
        List<Component> components = event.getToolTip();
        CustomData data = itemStack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            CompoundTag composition = tag.getCompound("composition");
            components.add(Component.literal("composition").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
            for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
                if (composition.contains(resourceLocation.toString())) {
                    float concentration = composition.getFloat(resourceLocation.toString());
                    components.add(
                            Component.translatable(resourceLocation.toLanguageKey("nucleus")).withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(String.format(" : %.2f %%", concentration * 100)).withStyle(ChatFormatting.GRAY)));
                }
            }
        }

    }
}
