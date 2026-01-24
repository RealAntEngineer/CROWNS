package com.rae.crowns.content.nuclear.fuel_assembly;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.content.rendering.RGBAVolumeInstance;
import com.rae.crowns.content.rendering.util.VolumeCubeMesh;
import com.rae.crowns.init.client.ShaderInit;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class AssemblyBERenderer extends SafeBlockEntityRenderer<AssemblyBlockEntity> {
    static RGBAVolumeInstance tcherenkov;
    static {
        RenderSystem.recordRenderCall(AssemblyBERenderer::initializeClientTcherenkov);

    }
    private static void initializeClientTcherenkov() {
        // Create simple 16^3 blue cube with density scaling with distance and centered in 0.5,0.5,0.5
        int Nx = 4, Ny = 4, Nz = 4;
        int brickSize = 4;
        float[] volumeRGBA = new float[Nx * Ny * Nz * 4];

        for (int z = 0; z < Nz; z++) {
            float fz = (z + 0.5f) / Nz - 0.5f; // center at 8
            for (int y = 0; y < Ny; y++) {
                float fy = (y + 0.5f) / Ny - 0.5f; // center at 8
                for (int x = 0; x < Nx; x++) {
                    float fx = (x + 0.5f) / Nx - 0.5f; // center at 8

                    //float scaling = (1+3 * 0.25f)/(1+fz*fz + fy*fy + fx*fx);//Mth.clamp((float) Math.cos((fz*fz + fy*fy + fx*fx)/(3 * 0.25)/Math.PI/2* 2), 0, 1)*0.5f;
                    int idx = x + y*Nx + z*Nx*Ny;
                    int off = idx * 4;

                    if (fx * fx < 0.25 * 0.25 && fy * fy < 0.25 * 0.25 && fz * fz < 0.25 * 0.25){
                        volumeRGBA[off] = 0f; // R
                        volumeRGBA[off + 1] = 0f; // G
                        volumeRGBA[off + 2] = 0;       // B
                        volumeRGBA[off + 3] = 0f;        // A

                    } else {
                        volumeRGBA[off] = 0.2f; // R
                        volumeRGBA[off + 1] = 0.67f; // G
                        volumeRGBA[off + 2] = 0.9f;       // B
                        volumeRGBA[off + 3] = 0.1f;//1f;        // A

                    }

                }
            }
        }


        tcherenkov = new RGBAVolumeInstance(volumeRGBA, Nx, Ny, Nz, brickSize);
        tcherenkov.position = new Vec3(-0.5, -0.5, -0.5);//start pos
        tcherenkov.size = new Vec3(2.0, 2.0, 2.0);
        //VolumeWorldRenderer.add(tcherenkov);
    }

    public AssemblyBERenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(AssemblyBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {

        float scaling = Math.min(be.getRadioactiveActivity()/1000, 1);
        if (scaling > 0.01f) {
            RenderSystem.setShader(() -> ShaderInit.volumeShader);
            RenderSystem.enableBlend();
            //RenderSystem.defaultBlendFunc();
            //RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            //RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.DST_ALPHA);
            RenderSystem.depthMask(false); // don't write depth
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();

            ms.pushPose();
            tcherenkov.opacityScale = scaling;

            if (VolumeCubeMesh.VBO == null) VolumeCubeMesh.init();
            assert Minecraft.getInstance().cameraEntity != null;
            BlockPos pos = be.getBlockPos();
            Vec3 cameraPos = Minecraft.getInstance().cameraEntity.getPosition(partialTicks);

            tcherenkov.position = new Vec3(pos.getX()-0.5f, pos.getY()-0.5f, pos.getZ()-0.5f);


            tcherenkov.render(ms, ShaderInit.volumeShader, cameraPos);

            ms.scale((float) (1/tcherenkov.size.x), (float) (1/tcherenkov.size.y), (float) (1/tcherenkov.size.z));

            ms.translate(cameraPos.x - pos.getX(), cameraPos.y - pos.getX(), cameraPos.z - pos.getZ());

            RenderSystem.depthMask(true); // don't write depth
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            ms.popPose();
        }
    }
}
