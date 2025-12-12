package com.rae.crowns.content.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class VolumeWorldRenderer {

    private static final List<VolumeInstance> volumes = new ArrayList<>();

    public static void add(VolumeInstance volume) {
        volumes.add(volume);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, ShaderInstance shader, Vec3 cameraPos, int maxSteps) {
        RenderSystem.setShader(() -> shader);
        if (VolumeCubeMesh.VBO == null) VolumeCubeMesh.init();
        for (VolumeInstance v : volumes) {
            v.render(poseStack, shader, cameraPos, maxSteps);
        }

        //RenderSystem.renderThreadTesselator().end();
    }
}

