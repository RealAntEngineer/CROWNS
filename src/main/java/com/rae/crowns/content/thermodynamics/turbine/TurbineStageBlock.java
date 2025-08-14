package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.colony_api.multiblock.MBKineticController;
import com.rae.colony_api.multiblock.MBStructureBlock;
import com.rae.crowns.init.client.ShapesInit;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class TurbineStageBlock extends MBKineticController implements IBE<TurbineStageBlockEntity> {
    public TurbineStageBlock(Properties pProperties, MBStructureBlock structure) {
        super(pProperties, structure);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return  Shapes.join(ShapesInit.TURBINE.get(state.getValue(FACING)), Shapes.block(), BooleanOp.AND);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return true;
    }
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
    @Override
    public boolean showCapacityWithAnnotation() {
        return true;
    }

    @Override
    public Class<TurbineStageBlockEntity> getBlockEntityClass() {
        return TurbineStageBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineStageBlockEntity> getBlockEntityType() {
        return BlockEntityInit.TURBINE_STAGE.get();
    }
    public static Couple<Integer> getSpeedRange() {
        return Couple.create(1, 16);
    }

    @Override
    public VoxelShape getGlobalShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return ShapesInit.TURBINE.get(state.getValue(FACING));
    }

    @Override
    public Vec3i getDefaultOffset() {
        return new Vec3i(0,1,1);
    }

    @Override
    public Vec3i getDefaultSize() {
        return new Vec3i(1,3,3);
    }
}
