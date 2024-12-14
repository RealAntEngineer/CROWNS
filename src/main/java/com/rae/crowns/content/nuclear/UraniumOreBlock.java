package com.rae.crowns.content.nuclear;

import com.mojang.math.Vector3f;
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

/**
 * copied from RedstoneOreBlock
 */
public class UraniumOreBlock extends Block {
   public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

   public UraniumOreBlock(Properties p_55453_) {
      super(p_55453_);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.valueOf(false)));
   }

   public void attack(BlockState p_55467_, Level p_55468_, BlockPos p_55469_, Player p_55470_) {
      interact(p_55467_, p_55468_, p_55469_);
      super.attack(p_55467_, p_55468_, p_55469_, p_55470_);
   }

   public void stepOn(Level p_154299_, BlockPos p_154300_, BlockState p_154301_, Entity p_154302_) {
      if (!p_154302_.isSteppingCarefully()) {
         interact(p_154301_, p_154299_, p_154300_);
      }

      super.stepOn(p_154299_, p_154300_, p_154301_, p_154302_);
   }

   public InteractionResult use(BlockState p_55472_, Level p_55473_, BlockPos p_55474_, Player p_55475_, InteractionHand p_55476_, BlockHitResult p_55477_) {
      if (p_55473_.isClientSide) {
         spawnParticles(p_55473_, p_55474_);
      } else {
         interact(p_55472_, p_55473_, p_55474_);
      }

      ItemStack itemstack = p_55475_.getItemInHand(p_55476_);
      return itemstack.getItem() instanceof BlockItem && (new BlockPlaceContext(p_55475_, p_55476_, itemstack, p_55477_)).canPlace() ? InteractionResult.PASS : InteractionResult.SUCCESS;
   }

   private static void interact(BlockState p_55493_, Level p_55494_, BlockPos p_55495_) {
      spawnParticles(p_55494_, p_55495_);
      if (!p_55493_.getValue(LIT)) {
         p_55494_.setBlock(p_55495_, p_55493_.setValue(LIT, Boolean.valueOf(true)), 3);
      }

   }

   public boolean isRandomlyTicking(BlockState p_55486_) {
      return p_55486_.getValue(LIT);
   }

   public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos pos, RandomSource randomSource) {
      if (blockState.getValue(LIT)) {
         serverLevel.setBlock(pos, blockState.setValue(LIT, Boolean.valueOf(false)), 3);
      }

   }

   public void spawnAfterBreak(BlockState blockState, ServerLevel serverLevel, BlockPos pos, ItemStack itemStack, boolean b) {
      super.spawnAfterBreak(blockState, serverLevel, pos, itemStack, b);
   }

   @Override
   public int getExpDrop(BlockState state, net.minecraft.world.level.LevelReader world, RandomSource randomSource, BlockPos pos, int fortune, int silktouch) {
      return silktouch == 0 ? 1 + randomSource.nextInt(5) : 0;
   }

   public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
      if (blockState.getValue(LIT)) {
         spawnParticles(level, blockPos);
      }

   }

   private static void spawnParticles(Level level, BlockPos pos) {
      double d0 = 0.5625D;
      RandomSource randomsource = level.random;

      for(Direction direction : Direction.values()) {
         BlockPos blockpos = pos.relative(direction);
         if (!level.getBlockState(blockpos).isSolidRender(level, blockpos)) {
            Direction.Axis direction$axis = direction.getAxis();
            double d1 = direction$axis == Direction.Axis.X ? 0.5D + d0 * (double)direction.getStepX() : (double)randomsource.nextFloat();
            double d2 = direction$axis == Direction.Axis.Y ? 0.5D + d0 * (double)direction.getStepY() : (double)randomsource.nextFloat();
            double d3 = direction$axis == Direction.Axis.Z ? 0.5D + d0 * (double)direction.getStepZ() : (double)randomsource.nextFloat();
            level.addParticle(new DustParticleOptions( new Vector3f(Vec3.fromRGB24(0x0cd628)), 1.0F), (double)pos.getX() + d1, (double)pos.getY() + d2, (double)pos.getZ() + d3, 0.0D, 0.0D, 0.0D);
         }
      }

   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LIT);
   }
}
