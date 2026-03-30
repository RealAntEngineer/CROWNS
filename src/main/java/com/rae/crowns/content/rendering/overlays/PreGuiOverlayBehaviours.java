package com.rae.crowns.content.rendering.overlays;

import com.rae.crowns.init.misc.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.jetbrains.annotations.NotNull;

public final class PreGuiOverlayBehaviours {
    public static void dosimeterGameOverlay(RenderGuiOverlayEvent.Pre event, Minecraft minecraft) {
        if (VanillaGuiOverlay.CROSSHAIR.type() == event.getOverlay()) {
            @NotNull LocalPlayer player = minecraft.player;

            if (player.getMainHandItem().getItem() == ItemInit.DOSIMETER.get() || player.getOffhandItem().getItem() == ItemInit.DOSIMETER.get()) {
               event.setCanceled(true);
            }
        }
    }
}
