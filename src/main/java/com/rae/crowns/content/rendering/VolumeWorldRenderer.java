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

    public static void render(PoseStack poseStack, MultiBufferSource buffers, ShaderInstance shader, Vec3 cameraPos, int maxSteps) {
        RenderSystem.setShader(() -> shader);
        RenderSystem.enableBlend();
        //RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        RenderSystem.depthMask(false); // don't write depth
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        if (VolumeCubeMesh.VBO == null) VolumeCubeMesh.init();
        synchronized(GLGuard.GL_LOCK) {
            for (RGBAVolumeInstance v : volumes) {
                v.render(poseStack, shader, cameraPos, maxSteps);
            }
        }
        RenderSystem.depthMask(true); // don't write depth
        RenderSystem.defaultBlendFunc();

        //if (buffers instanceof MultiBufferSource.BufferSource bufferSource)bufferSource.endBatch();

        //RenderSystem.renderThreadTesselator().end();
    }

    public static void remove(RGBAVolumeInstance tcherenkov) {
        volumes.remove(tcherenkov);
    }
}

