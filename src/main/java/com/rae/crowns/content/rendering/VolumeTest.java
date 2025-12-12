package com.rae.crowns.content.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import static com.rae.crowns.init.client.ShaderInit.volumeShader;
import static net.minecraftforge.client.event.RenderLevelStageEvent.Stage.*;

@Mod.EventBusSubscriber(modid = "crowns", value = Dist.CLIENT)
public class VolumeTest {

    private static boolean initialized = false;

    /** Initialize once: create volume and shader */
    public static void init() {
        if (initialized) return;

        // Create simple 16^3 red cube with alpha 0.5
        int Nx = 32, Ny = 32, Nz = 32;
        int brickSize = 4;
        float[] volumeRGBA = new float[Nx * Ny * Nz * 4];

        // Torus parameters
        float R = 0.25f; // major radius (distance from center to tube center)
        float r = 0.2f; // minor radius (tube radius)
        float a; // opacity scaling

        for (int z = 0; z < Nz; z++) {
            float fz = (z + 0.5f) / Nz - 0.5f; // center at 8
            for (int y = 0; y < Ny; y++) {
                float fy = (y + 0.5f) / Ny - 0.5f; // center at 8
                for (int x = 0; x < Nx; x++) {
                    float fx = (x + 0.5f) / Nx - 0.5f; // center at 8

                    // Torus equation: sqrt(x^2 + z^2) = R ± r
                    float dxz = (float)Math.sqrt(fx*fx + fz*fz);
                    float dist = Math.abs(dxz - R); // distance from torus circle in XZ plane
                    float dy = fy;

                    float d = (float)Math.sqrt(dist*dist + dy*dy); // distance from torus surface

                    int idx = x + y*Nx + z*Nx*Ny;
                    int off = idx * 4;
                    a = r/(Math.min(d,0.01f)*10);
                    if (d < r) {
                        // inside torus tube: color + alpha
                        volumeRGBA[off]     = 0.7f ; // R
                        volumeRGBA[off + 1] = 0.4f; // G
                        volumeRGBA[off + 2] = 1f;       // B
                        volumeRGBA[off + 3] = a * a;        // A
                    } else {
                        // outside: transparent
                        volumeRGBA[off]     = 0;
                        volumeRGBA[off + 1] = 0;
                        volumeRGBA[off + 2] = 0f;
                        volumeRGBA[off + 3] = 0;
                    }
                }
            }
        }


        VolumeInstance volume = new VolumeInstance(volumeRGBA, Nx, Ny, Nz, brickSize);
        volume.position = new Vec3(1, 1, 1);
        volume.size = new Vec3(2.0, 2.0, 2.0);

        VolumeWorldRenderer.add(volume);

        initialized = true;
    }

    /** Render every frame */
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!initialized){
            init();
        }
        if (volumeShader == null || event.getStage() != AFTER_BLOCK_ENTITIES) return;

        PoseStack poseStack = event.getPoseStack();
        SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();

        Vec3 cameraPos = event.getCamera().getPosition();
        int maxSteps = 256;
        float stepScale = 5f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        //debugRenderSolidCube(poseStack, buffers, cameraPos);
        VolumeWorldRenderer.render(poseStack, buffers, volumeShader, cameraPos, maxSteps);

        // Finish the batch
        //buffers();

        buffers.draw();

    }

    public static void debugRenderSolidCube(PoseStack poseStack, MultiBufferSource buffers, Vec3 cameraPos) {
        poseStack.pushPose();

        // ✅ Convert from world space → view space
        poseStack.translate(
                -cameraPos.x,
                -cameraPos.y,
                -cameraPos.z
        );
        //miss the camera rotations

        // ✅ Cube is now centered exactly at world (0, 0, 0)
        poseStack.scale(1f, 1f, 1f);

        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f mat = poseStack.last().pose();

        int light = 0xF000F0; // full brightness
        int overlay = 0;

        // -------- FRONT (+Z)
        vc.vertex(mat, 0, 0, 1).color(255, 0, 0, 255).uv(0, 0).uv2(light).overlayCoords(overlay).normal(0, 0, 1).endVertex();
        vc.vertex(mat, 1, 0, 1).color(255, 0, 0, 255).uv(1, 0).uv2(light).overlayCoords(overlay).normal(0, 0, 1).endVertex();
        vc.vertex(mat, 1, 1, 1).color(255, 0, 0, 255).uv(1, 1).uv2(light).overlayCoords(overlay).normal(0, 0, 1).endVertex();
        vc.vertex(mat, 0, 1, 1).color(255, 0, 0, 255).uv(0, 1).uv2(light).overlayCoords(overlay).normal(0, 0, 1).endVertex();

        // -------- BACK (-Z)
        vc.vertex(mat, 1, 0, 0).color(0, 255, 0, 255).uv(0, 0).uv2(light).overlayCoords(overlay).normal(0, 0, -1).endVertex();
        vc.vertex(mat, 0, 0, 0).color(0, 255, 0, 255).uv(1, 0).uv2(light).overlayCoords(overlay).normal(0, 0, -1).endVertex();
        vc.vertex(mat, 0, 1, 0).color(0, 255, 0, 255).uv(1, 1).uv2(light).overlayCoords(overlay).normal(0, 0, -1).endVertex();
        vc.vertex(mat, 1, 1, 0).color(0, 255, 0, 255).uv(0, 1).uv2(light).overlayCoords(overlay).normal(0, 0, -1).endVertex();

        // -------- LEFT (-X)
        vc.vertex(mat, 0, 0, 0).color(0, 0, 255, 255).uv(0, 0).uv2(light).overlayCoords(overlay).normal(-1, 0, 0).endVertex();
        vc.vertex(mat, 0, 0, 1).color(0, 0, 255, 255).uv(1, 0).uv2(light).overlayCoords(overlay).normal(-1, 0, 0).endVertex();
        vc.vertex(mat, 0, 1, 1).color(0, 0, 255, 255).uv(1, 1).uv2(light).overlayCoords(overlay).normal(-1, 0, 0).endVertex();
        vc.vertex(mat, 0, 1, 0).color(0, 0, 255, 255).uv(0, 1).uv2(light).overlayCoords(overlay).normal(-1, 0, 0).endVertex();

        // -------- RIGHT (+X)
        vc.vertex(mat, 1, 0, 1).color(255, 255, 0, 255).uv(0, 0).uv2(light).overlayCoords(overlay).normal(1, 0, 0).endVertex();
        vc.vertex(mat, 1, 0, 0).color(255, 255, 0, 255).uv(1, 0).uv2(light).overlayCoords(overlay).normal(1, 0, 0).endVertex();
        vc.vertex(mat, 1, 1, 0).color(255, 255, 0, 255).uv(1, 1).uv2(light).overlayCoords(overlay).normal(1, 0, 0).endVertex();
        vc.vertex(mat, 1, 1, 1).color(255, 255, 0, 255).uv(0, 1).uv2(light).overlayCoords(overlay).normal(1, 0, 0).endVertex();

        // -------- TOP (+Y)
        vc.vertex(mat, 0, 1, 1).color(0, 255, 255, 255).uv(0, 0).uv2(light).overlayCoords(overlay).normal(0, 1, 0).endVertex();
        vc.vertex(mat, 1, 1, 1).color(0, 255, 255, 255).uv(1, 0).uv2(light).overlayCoords(overlay).normal(0, 1, 0).endVertex();
        vc.vertex(mat, 1, 1, 0).color(0, 255, 255, 255).uv(1, 1).uv2(light).overlayCoords(overlay).normal(0, 1, 0).endVertex();
        vc.vertex(mat, 0, 1, 0).color(0, 255, 255, 255).uv(0, 1).uv2(light).overlayCoords(overlay).normal(0, 1, 0).endVertex();

        // -------- BOTTOM (-Y)
        vc.vertex(mat, 0, 0, 0).color(255, 0, 255, 255).uv(0, 0).uv2(light).overlayCoords(overlay).normal(0, -1, 0).endVertex();
        vc.vertex(mat, 1, 0, 0).color(255, 0, 255, 255).uv(1, 0).uv2(light).overlayCoords(overlay).normal(0, -1, 0).endVertex();
        vc.vertex(mat, 1, 0, 1).color(255, 0, 255, 255).uv(1, 1).uv2(light).overlayCoords(overlay).normal(0, -1, 0).endVertex();
        vc.vertex(mat, 0, 0, 1).color(255, 0, 255, 255).uv(0, 1).uv2(light).overlayCoords(overlay).normal(0, -1, 0).endVertex();

        poseStack.popPose();
    }


}
