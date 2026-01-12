package com.rae.crowns.content.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.content.rendering.util.GLGuard;
import com.rae.crowns.content.rendering.util.VolumeCubeMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class VolumeWorldRenderer {

    private static final Set<RGBAVolumeInstance> volumes = new HashSet<>();

    public static void add(RGBAVolumeInstance volume) {
        volumes.add(volume);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, ShaderInstance shader, Vec3 cameraPos) {
        RenderSystem.setShader(() -> shader);
        RenderSystem.enableBlend();
        //RenderSystem.defaultBlendFunc();
        //RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        //RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.DST_ALPHA);
        RenderSystem.depthMask(false); // don't write depth
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        if (VolumeCubeMesh.VBO == null) VolumeCubeMesh.init();
        synchronized(GLGuard.GL_LOCK) {
            for (RGBAVolumeInstance v : volumes) {
                v.render(poseStack, shader, cameraPos);
            }
        }
        RenderSystem.depthMask(true); // don't write depth
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        //if (buffers instanceof MultiBufferSource.BufferSource bufferSource)bufferSource.endBatch();

        //RenderSystem.renderThreadTesselator().end();
    }

    public static void remove(RGBAVolumeInstance tcherenkov) {
        volumes.remove(tcherenkov);
    }
}

