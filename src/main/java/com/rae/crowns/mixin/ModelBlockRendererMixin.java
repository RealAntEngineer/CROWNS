package com.rae.crowns.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rae.crowns.content.fields.util.client.TemperatureColorCache;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

        @Redirect(
                method = "putQuadData",
                at = @At(
                        value = "INVOKE",
                        target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFF[IIZ)V"
                )
        )
        private void crowns$putBulkDataWithTemperatureTint(
                VertexConsumer consumer,
                PoseStack.Pose pose,
                BakedQuad quad,
                float[] brightness,
                float r, float g, float b,
                int[] light,
                int overlay,
                boolean useAmbientOcclusion
        ) {
            var tint = TemperatureColorCache.getCurrentTint();

            consumer.putBulkData(
                    pose,
                    quad,
                    brightness,
                    r * (float) tint.x,
                    g * (float) tint.y,
                    b * (float) tint.z,
                    light,
                    overlay,
                    useAmbientOcclusion
            );
        }

}