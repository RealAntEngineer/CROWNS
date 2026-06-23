package com.rae.crowns.content.nuclear.rod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.lwjgl.system.NonnullDefault;

import static com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;
import static com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer.KINETIC_BLOCK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

@NonnullDefault
public class RodRenderer extends SafeBlockEntityRenderer<RodBlockEntity> {


    public RodRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    protected void renderSafe(RodBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
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

    protected BlockState getRenderedBlockState(RodBlockEntity be) {
        return be.getBlockState();
    }
}