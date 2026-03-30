package com.rae.crowns.content.event;

import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.sound.CrownsSoundScapes;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onTick(TickEvent.@NotNull ClientTickEvent event) {
        if (!isGameActive())
            return;

        Level world = Minecraft.getInstance().level;
        assert world != null;
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        CrownsSoundScapes.tick();
        SteamFlowManager.tick(world);
    }

    protected static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }

    @SubscribeEvent
    public static void addToItemTooltip(@NotNull ItemTooltipEvent event) {
        if (event.getEntity() == null)
            return;

        ItemStack       itemStack   = event.getItemStack();
        List<Component> components  = event.getToolTip();
        CompoundTag     composition = itemStack.getTagElement("composition");
        if (composition != null) {
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
