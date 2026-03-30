package com.rae.crowns.content.rendering.overlays;

import com.mojang.blaze3d.platform.Window;
import com.rae.crowns.init.misc.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@SuppressWarnings("ConstantConditions") // Shut the fuck up
public final class PostGuiOverlayBehaviours {
    private static HashMap<LocalPlayer, Integer> dosimeterTweeningMap = new HashMap<>(); // Stores steps

    public static void dosimeterGuiOverlay(GuiGraphics guiGraphics, Minecraft minecraft) {
        final ResourceLocation dosimeterReticleTexture = new ResourceLocation("crowns", "textures/gui/dosimeter_reticle.png");
        final ResourceLocation dosimeterSliderTexture = new ResourceLocation("crowns", "textures/gui/dosimeter_slider.png");

        final int steps = 20;

        @NotNull LocalPlayer player = minecraft.player;

        if (player.getMainHandItem().getItem() != ItemInit.DOSIMETER.get() && player.getOffhandItem().getItem() != ItemInit.DOSIMETER.get()) {
            dosimeterTweeningMap.put(player, 0);
            return;
        }

        Window window = minecraft.getWindow();

        int windowWidth = window.getGuiScaledWidth();
        int windowHeight = window.getGuiScaledHeight();

        int width = windowWidth / 3;
        int height = width / 2; // Aspect ratio 2:1

        int x = windowWidth / 2 - (width / 2);
        int y;

        int step = dosimeterTweeningMap.get(player);
        if (step < steps) {
            dosimeterTweeningMap.put(player, step + 1);

            int destination_y = (windowHeight / 2 - (height / 2));
            y = destination_y + (destination_y / step);
        } else {
            int pre_y = (windowHeight / 2 - (height / 2));
            y = pre_y + (pre_y / steps); // So it doesn't "jump" on the last step
        }

        guiGraphics.blit(
                dosimeterReticleTexture,
                x, y,
                0, 0,
                width, height,
                width, height
        );
    }
}
