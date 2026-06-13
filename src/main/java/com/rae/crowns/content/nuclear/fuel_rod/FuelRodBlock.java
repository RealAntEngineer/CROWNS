package com.rae.crowns.content.nuclear.fuel_rod;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FuelRodBlock extends Block implements EntityBlock, IBE<FuelRodBlockEntity> {

    public enum Activity {
        NONE, LOW, HIGH
    }

    public static final EnumProperty<Activity> ACTIVITY = EnumProperty.create("activity", Activity.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    protected static final VoxelShape SHAPE =
            Shapes.box(3 / 16f, 0, 3 / 16f, 13 / 16f, 1, 13 / 16f);

    public FuelRodBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(ACTIVITY, Activity.NONE)
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public Class<FuelRodBlockEntity> getBlockEntityClass() {
        return FuelRodBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FuelRodBlockEntity> getBlockEntityType() {
        return BlockEntityInit.FUEL_ROD.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelRodBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BlockEntityInit.FUEL_ROD.get() ? FuelRodBlockEntity::tick : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVITY, FACING);
    }
}
