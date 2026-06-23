package com.rae.crowns.content.nuclear.rod;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class RodDriverRenderer extends KineticBlockEntityRenderer<RodDriverBlockEntity> {

    public RodDriverRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected BlockState getRenderedBlockState(RodDriverBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

}