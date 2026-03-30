package com.rae.crowns.content.rendering.overlays;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class GuiOverlays {
    public static void dosimeterGuiOverlay(GuiGraphics guiGraphics, Minecraft minecraft) {
        final ResourceLocation dosimeterReticleTexture = new ResourceLocation("crowns", "textures/gui/dosimeter_reticle.png");
        final ResourceLocation dosimeterSliderTexture = new ResourceLocation("crowns", "textures/gui/dosimeter_slider.png");

        Window window = minecraft.getWindow();

        int windowWidth = window.getGuiScaledWidth();
        int windowHeight = window.getGuiScaledHeight();

        int width = windowWidth / 5;
        int height = width / 2; // Aspect ratio 2:1

        int x = 10;
        int y = 10;

        guiGraphics.blit(
                dosimeterReticleTexture,
                x, y,
                0, 0,
                width, height,
                width, height
        );
    }
}
