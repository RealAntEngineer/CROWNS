package com.rae.crowns.content.nuclear.rod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class GraphiteSleeveRenderer extends SafeBlockEntityRenderer<GraphiteSleeveBlockEntity> {


    public GraphiteSleeveRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    protected void renderSafe(GraphiteSleeveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        float offset = be.getInterpolatedOffset(partialTicks);

        BlockState state = getRenderedBlockState(be);
        RenderType type  = RenderType.cutoutMipped();
        Direction.Axis axis = state.getValue(RodBlock.AXIS);

        //render the block but offset in the axis by offset

        VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer rodModel = CachedBuffers.partial(((RodBlock)state.getBlock()).getRodModel(), state);

        rodModel.center()
                .rotateYDegrees(axis == Direction.Axis.Y ? 0 : 90)
                .rotateXDegrees(axis == Direction.Axis.X ? 90 : axis == Direction.Axis.Z ? 180 : 0)
                .translate(0, offset, 0)
                .uncenter()
                .light(light)
                .renderInto(ms, vb);
    }

    protected BlockState getRenderedBlockState(GraphiteSleeveBlockEntity be) {
        return be.getBlockState();
    }
}