package com.rae.crowns.client.rendering.renderer;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rae.crowns.content.thermodynamics.HeatExchangerBlock;
import com.rae.crowns.content.thermodynamics.HeatExchangerBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlockEntity;
import com.rae.crowns.init.PartialModelInit;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class HeatExchangerRenderer extends SafeBlockEntityRenderer<HeatExchangerBlockEntity> {
    public HeatExchangerRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    protected void renderSafe(HeatExchangerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        //if (Backend.canUseInstancing(be.getLevel())) return;

        //super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        BlockState state = be.getBlockState();

        Direction direction = state.getValue(HeatExchangerBlock.FACING);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        if (state.getValue(HeatExchangerBlock.OUT)) {
            ms.pushPose();
            ms.translate(direction.getStepX()* 0.001f,direction.getStepY()*0.001f,direction.getStepZ()*0.001f);
            SuperByteBuffer outRim =
                    CachedBufferer.partialFacing(AllPartialModels.PIPE_ATTACHMENTS.get(
                            FluidTransportBehaviour.AttachmentTypes.ComponentPartials.RIM).get(direction), be.getBlockState(), Direction.SOUTH);
            outRim.renderInto(ms, vb);

            ms.popPose();

        }
        if (state.getValue(HeatExchangerBlock.IN)) {
            ms.pushPose();
            ms.translate(direction.getOpposite().getStepX()* 0.001f,direction.getOpposite().getStepY()*0.001f,
                    direction.getOpposite().getStepZ()*0.001f);
            SuperByteBuffer inRim =
                    CachedBufferer.partialFacing(AllPartialModels.PIPE_ATTACHMENTS.get(
                                    FluidTransportBehaviour.AttachmentTypes.ComponentPartials.RIM).get(direction.getOpposite()), be.getBlockState(),
                            Direction.SOUTH);
            inRim.renderInto(ms, vb);
            ms.popPose();
        }
    }
}
