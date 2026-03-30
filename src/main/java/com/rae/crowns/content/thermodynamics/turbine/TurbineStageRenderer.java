package com.rae.crowns.content.thermodynamics.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rae.crowns.init.client.PartialModelInit;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TurbineStageRenderer extends KineticBlockEntityRenderer<TurbineStageBlockEntity> {
    public TurbineStageRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull TurbineStageBlockEntity be, float partialTicks, @NotNull PoseStack ms, @NotNull MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        //super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        BlockState state = be.getBlockState();

        Direction      direction = Direction.fromAxisAndDirection(((TurbineStageBlock) state.getBlock()).getRotationAxis(state), Direction.AxisDirection.POSITIVE);
        VertexConsumer vb        = buffer.getBuffer(RenderType.cutoutMipped());
        ms.pushPose();
        SuperByteBuffer memoryRoll =
                CachedBuffers.partialFacing(PartialModelInit.TURBINE_STAGE, be.getBlockState(), direction.getOpposite());
        standardKineticRotationTransform(memoryRoll, be, light).renderInto(ms, vb);
        ms.popPose();
    }
}
