package com.rae.crowns.content.nuclear.rod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class RodDriverRenderer extends KineticBlockEntityRenderer<RodDriverBlockEntity> {

    public RodDriverRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RodDriverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        float offset = be.getInterpolatedOffset(partialTicks);

        Direction.Axis axis = be.getAxis();

        //render the block but offset in the axis by offset

        VertexConsumer vb  = buffer.getBuffer(RenderType.solid());
        RodBlock       rod = be.getRodContained();
        if (rod != null) {
            SuperByteBuffer rodModel = CachedBuffers.partial(rod.getRodModel(), rod.defaultBlockState());

            assert be.getLevel() != null;
            rodModel.center()
                    .rotateYDegrees(axis == Direction.Axis.X ? 90 : 0)
                    .rotateXDegrees(axis == Direction.Axis.Y ? 0 : 90)
                    .translate(0, offset, 0)
                    .uncenter()
                    .light(Math.max(LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(
                            axis, offset < 0 ? -1 : 1
                    )), light))
                    .renderInto(ms, vb);
        }
    }

    @Override
    protected BlockState getRenderedBlockState(RodDriverBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

}