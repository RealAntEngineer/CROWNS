package com.rae.crowns.content.nuclear.uranium;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;


/**
 * copied from RedstoneOreBlock
 */
public class UraniumOreBlock extends Block {
    public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

    public UraniumOreBlock(@NotNull Properties p_55453_) {
        super(p_55453_);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.FALSE));
    }

    private static void interact(@NotNull BlockState p_55493_, @NotNull Level p_55494_, @NotNull BlockPos p_55495_) {
        spawnParticles(p_55494_, p_55495_);
        if (!p_55493_.getValue(LIT)) {
            p_55494_.setBlock(p_55495_, p_55493_.setValue(LIT, Boolean.TRUE), 3);
        }

    }

    private static void spawnParticles(@NotNull Level level, @NotNull BlockPos pos) {
        double d0 = 0.5625D;
        RandomSource randomsource = level.random;

        for (Direction direction : Direction.values()) {
            BlockPos blockpos = pos.relative(direction);
            if (!level.getBlockState(blockpos).isSolidRender(level, blockpos)) {
                Direction.Axis direction$axis = direction.getAxis();
                double d1 = direction$axis == Direction.Axis.X ? 0.5D + d0 * (double) direction.getStepX() : (double) randomsource.nextFloat();
                double d2 = direction$axis == Direction.Axis.Y ? 0.5D + d0 * (double) direction.getStepY() : (double) randomsource.nextFloat();
                double d3 = direction$axis == Direction.Axis.Z ? 0.5D + d0 * (double) direction.getStepZ() : (double) randomsource.nextFloat();
                level.addParticle(new DustParticleOptions(new Vector3f(Vec3.fromRGB24(0x0cd628).toVector3f()), 1.0F), (double) pos.getX() + d1, (double) pos.getY() + d2, (double) pos.getZ() + d3, 0.0D, 0.0D, 0.0D);
            }
        }

    }

    public void attack(BlockState p_55467_, Level p_55468_, BlockPos p_55469_, Player p_55470_) {
        interact(p_55467_, p_55468_, p_55469_);
        super.attack(p_55467_, p_55468_, p_55469_, p_55470_);
    }

    public void stepOn(Level p_154299_, BlockPos p_154300_, BlockState p_154301_, @NotNull Entity p_154302_) {
        if (!p_154302_.isSteppingCarefully()) {
            interact(p_154301_, p_154299_, p_154300_);
        }

        super.stepOn(p_154299_, p_154300_, p_154301_, p_154302_);
    }

    public @NotNull InteractionResult use(BlockState p_55472_, @NotNull Level p_55473_, BlockPos p_55474_, Player p_55475_, InteractionHand p_55476_, BlockHitResult p_55477_) {
        if (p_55473_.isClientSide) {
            spawnParticles(p_55473_, p_55474_);
        } else {
            interact(p_55472_, p_55473_, p_55474_);
        }

        ItemStack itemstack = p_55475_.getItemInHand(p_55476_);
        return itemstack.getItem() instanceof BlockItem && (new BlockPlaceContext(p_55475_, p_55476_, itemstack, p_55477_)).canPlace() ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    public boolean isRandomlyTicking(@NotNull BlockState p_55486_) {
        return p_55486_.getValue(LIT);
    }

    public void randomTick(@NotNull BlockState blockState, ServerLevel serverLevel, BlockPos pos, RandomSource randomSource) {
        if (blockState.getValue(LIT)) {
            serverLevel.setBlock(pos, blockState.setValue(LIT, Boolean.FALSE), 3);
        }

    }

    public void spawnAfterBreak(BlockState blockState, ServerLevel serverLevel, BlockPos pos, ItemStack itemStack, boolean b) {
        super.spawnAfterBreak(blockState, serverLevel, pos, itemStack, b);
    }

    @Override
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelReader world, @NotNull RandomSource randomSource, BlockPos pos, int fortune, int silktouch) {
        return silktouch == 0 ? 1 + randomSource.nextInt(5) : 0;
    }

    public void animateTick(@NotNull BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
        if (blockState.getValue(LIT)) {
            spawnParticles(level, blockPos);
        }

    }

    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
