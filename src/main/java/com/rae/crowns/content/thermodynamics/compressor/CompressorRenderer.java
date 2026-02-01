package com.rae.crowns.content.thermodynamics.compressor;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class CompressorRenderer extends KineticBlockEntityRenderer<CompressorBlockEntity> {

    public CompressorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected @NotNull SuperByteBuffer getRotatedModel(CompressorBlockEntity be, @NotNull BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.MECHANICAL_PUMP_COG, state);
    }

}
