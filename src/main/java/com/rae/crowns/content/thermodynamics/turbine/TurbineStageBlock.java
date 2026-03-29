package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.client.ShapesInit;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.rae.formicapi.content.multiblock.MBKineticController;
import com.rae.formicapi.content.multiblock.MBStructureBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class TurbineStageBlock extends MBKineticController implements IBE<TurbineStageBlockEntity> {

    public static final BooleanProperty CASING = BooleanProperty.create("casing");

    public TurbineStageBlock(@NotNull Properties pProperties, MBStructureBlock structure) {
        super(pProperties, structure);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(CASING, true));
    }

    public static @NotNull Couple<Integer> getSpeedRange() {
        return Couple.create(1, 16);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(CASING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.join(ShapesInit.TURBINE.get(state.getValue(FACING)), Shapes.block(), BooleanOp.AND);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return true;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, @NotNull BlockState state, @NotNull Direction face) {
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.@NotNull Axis getRotationAxis(@NotNull BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean showCapacityWithAnnotation() {
        return true;
    }

    @Override
    public @NotNull Class<TurbineStageBlockEntity> getBlockEntityClass() {
        return TurbineStageBlockEntity.class;
    }

    @Override
    public @NotNull BlockEntityType<? extends TurbineStageBlockEntity> getBlockEntityType() {
        return BlockEntityInit.TURBINE_STAGE.get();
    }

    @Override
    public @NotNull VoxelShape getGlobalShape(@NotNull BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return ShapesInit.TURBINE.get(state.getValue(FACING));
    }

    @Override
    public @NotNull Vec3i getDefaultOffset() {
        return new Vec3i(0, 1, 1);
    }

    @Override
    public @NotNull Vec3i getDefaultSize() {
        return new Vec3i(1, 3, 3);
    }
}
