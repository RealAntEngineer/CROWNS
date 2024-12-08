package com.rae.crowns.content.thermodynamics.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpecialTextures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SteamCurrentRenderer extends EntityRenderer<SteamCurrent> {

    public SteamCurrentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull SteamCurrent atmosphere, @NotNull Frustum frustum, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }

    @Override
    public void render(@NotNull SteamCurrent roomAtmosphere, float cameraX, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int p_114604_) {
        super.render(roomAtmosphere,cameraX,partialTick,poseStack,bufferSource,p_114604_);
    }

    @Override
    public ResourceLocation getTextureLocation(SteamCurrent atmosphere) {
        return AllSpecialTextures.BLANK.getLocation();
    }
}
