package com.rae.crowns.content.nuclear.fuel_feeder;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FuelFeederBlock extends Block implements EntityBlock, IBE<FuelFeederBlockEntity> {

    protected static final VoxelShape SHAPE =
            Shapes.box(2 / 16f, 0, 2 / 16f, 14 / 16f, 1, 14 / 16f);

    public FuelFeederBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<FuelFeederBlockEntity> getBlockEntityClass() {
        return FuelFeederBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FuelFeederBlockEntity> getBlockEntityType() {
        return BlockEntityInit.FUEL_FEEDER.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelFeederBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BlockEntityInit.FUEL_FEEDER.get() ? FuelFeederBlockEntity::tick : null;
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FuelFeederBlockEntity feeder) {
            return feeder.tryInsertFromPlayer(player, hand)
                    ? InteractionResult.CONSUME
                    : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }
}
