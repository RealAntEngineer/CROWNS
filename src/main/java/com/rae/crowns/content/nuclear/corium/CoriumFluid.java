package com.rae.crowns.content.nuclear.corium;


import com.rae.crowns.init.misc.BlockInit;
import com.rae.crowns.init.misc.TagsInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.lwjgl.system.NonnullDefault;

import java.util.ArrayList;

/**
 * this is corium, it follows the following spread rule:
 * - it tries to spread on the bottom and 4 sides
 * - it first transforms it into magma
 * - spreading to a block add a decay of 1
 * - spreading to a refactory block add a decay of 2
 */
@NonnullDefault
public abstract class CoriumFluid extends ForgeFlowingFluid {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);

    protected CoriumFluid(ForgeFlowingFluid.Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return super.createLegacyBlock(state).setValue(POWER, state.getValue(POWER));
    }

    public static class Flowing extends CoriumFluid {

        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7).setValue(POWER, 15));

        }

        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL).add(POWER);
        }

        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends CoriumFluid {
        public Source(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(POWER, 15));
        }

        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(POWER);
        }

        public int getAmount(FluidState state) {
            return 8;
        }

        public boolean isSource(FluidState state) {
            return true;
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, FluidState state) {
        //schedule tick ?
        //make neighbor blocks decay

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP)
                continue;
            BlockPos   adjacentPos   = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (!adjacentState.isAir() && (!adjacentState.liquid() && !TagsInit.CustomBlockTags.UNDESTRUCTABLE.matches(adjacentState))) {
                int power = state.getValue(POWER);
                if (power > 2 && (level.random.nextFloat() * 15 < power * (direction == Direction.DOWN ? 10 : 1))) {
                    state = this.getFlowing(state.getAmount(), power - 1, false);
                    //level.setBlock(pos, state.createLegacyBlock(), 3);
                    level.setBlock(adjacentPos, this.getFlowing(6, power - 2, false).createLegacyBlock(), 3);
                }
            }
        }
        //power diffusion
        int currentPower = state.getValue(POWER);
        for (Direction direction : Direction.values()) {
            FluidState adjState = level.getFluidState(pos.relative(direction));

            if (adjState.getFluidType().equals(this.getFluidType())) {
                int adjPower = adjState.getValue(POWER);
                if (adjPower + 1 < currentPower && currentPower > 1) {
                    level.setBlock(pos.relative(direction), this.getFlowing(adjState.getAmount(), adjPower + 1, false).createLegacyBlock(), 3);
                    currentPower -= 1;
                }
            } else if (adjState.is(FluidTags.WATER)) {
                level.setBlock(pos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
                currentPower -= 1;

            }
        }

        if (level.getRandom().nextFloat() <= 0.5f) {
            if (currentPower <= 1) {
                level.setBlock(pos, BlockInit.SOLID_CORIUM.getDefaultState(), 3);
                return;
            } else {
                currentPower = state.getValue(POWER) - 1;

            }
        }
        if (state.hasProperty(POWER)) {
            currentPower = Mth.clamp(currentPower, 0, 15);
            state.setValue(POWER, currentPower);
        }
        level.setBlock(pos, state.createLegacyBlock(), 3);
        this.spread(level, pos, state);
    }


    @Override
    protected int getSpreadDelay(Level level, BlockPos pos, FluidState currentState, FluidState newState) {
        return 20;
    }

    @Override
    protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState, Fluid fluid) {
        if (toBlockState.isAir() || toBlockState.liquid()) {
            return toFluidState.getType().isSame(this) || toFluidState.isEmpty() || toFluidState.is(FluidTags.WATER);
        }
        return false;
    }

    @Override
    @Deprecated
    public FluidState getFlowing(int level, boolean falling) {
        return getFlowing(level, 15, falling);
    }

    public FluidState getFlowing(int level, int power, boolean falling) {
        return this.getFlowing().defaultFluidState().setValue(LEVEL, level).setValue(POWER, power).setValue(FALLING, falling);
    }

    protected void spread(Level level, BlockPos pos, FluidState originalFluid) {

        if (!originalFluid.isEmpty()) {
            BlockState currentState    = level.getBlockState(pos);
            int        amountAvailable = originalFluid.getAmount();
            int        originalPower   = originalFluid.getValue(POWER);
            //if there is a space bellow or the same fluid we fall down.
            BlockPos   bellowPos       = pos.below();
            BlockState bellowState     = level.getBlockState(bellowPos);
            FluidState bellowFluid     = level.getFluidState(bellowPos);
            int        oldBellowAmount = bellowFluid.getAmount();
            if (oldBellowAmount < 8 &&
                    this.canSpreadTo(level, pos, currentState, Direction.DOWN, bellowPos, bellowState, bellowFluid, originalFluid.getType())) {
                //we try to fill completely the block bellow us.

                int newBellowAmount   = Mth.clamp(oldBellowAmount + originalFluid.getAmount(), 1, 8);
                int transmittedAmount = newBellowAmount - oldBellowAmount;
                int bellowPower       = bellowFluid.isEmpty() ? originalPower : bellowFluid.getValue(POWER);
                this.spreadTo(level, bellowPos, bellowState, Direction.DOWN,
                        this.getFlowing(newBellowAmount, Mth.clamp((transmittedAmount * originalPower + bellowPower * oldBellowAmount) / newBellowAmount, 0, 15), false));

                if (newBellowAmount - oldBellowAmount >= originalFluid.getAmount()) {
                    amountAvailable = 0;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                } else {

                    amountAvailable = originalFluid.getAmount() - transmittedAmount;

                    level.setBlock(pos, this.getFlowing(amountAvailable, originalPower, false).createLegacyBlock(), 3);
                }
            }
            if (amountAvailable > 0) {

                ArrayList<Direction> directionToSpread = new ArrayList<>();
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos   blockpos      = pos.relative(direction);
                    BlockState blockstate    = level.getBlockState(blockpos);
                    FluidState relativeState = level.getFluidState(blockpos);
                    if (this.canSpreadTo(level, pos, currentState, direction, blockpos, blockstate, relativeState, originalFluid.getType()) && relativeState.getAmount() < originalFluid.getAmount()) {
                        directionToSpread.add(direction);
                    }
                }
                if (!directionToSpread.isEmpty()) {
                    int spreadAmount = amountAvailable / (directionToSpread.size() + 1);//plus center
                    if (spreadAmount > 0) {

                        for (Direction direction : directionToSpread) {
                            BlockPos   blockpos     = pos.relative(direction);
                            BlockState blockstate   = level.getBlockState(blockpos);
                            FluidState fluidPresent = level.getFluidState(blockpos);
                            int        presentPower = fluidPresent.isEmpty() ? originalPower : fluidPresent.getValue(POWER);
                            int        newAmount    = Mth.clamp(spreadAmount + fluidPresent.getAmount(), 1, 8);
                            FluidState spreadState =
                                    this.getFlowing(newAmount,
                                            Mth.clamp((presentPower * fluidPresent.getAmount()
                                                    + spreadAmount * originalPower) / newAmount, 0, 15), false);

                            if (spreadState.getAmount() > fluidPresent.getAmount() && amountAvailable > spreadAmount) {
                                amountAvailable -= spreadAmount;
                                this.spreadTo(level, blockpos, blockstate, direction, spreadState);
                            }
                        }
                        //we remove the spent liquid from the originating block
                        level.setBlock(pos, this.getFlowing(amountAvailable, originalPower, false).createLegacyBlock(), 3);
                    }
                }
            }

        }
    }

    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
        if (blockState.getBlock() instanceof LiquidBlockContainer) {
            ((LiquidBlockContainer) blockState.getBlock()).placeLiquid(level, pos, blockState, fluidState);
        } else {
            if (!blockState.isAir()) {
                this.beforeDestroyingBlock(level, pos, blockState);
            }

            level.setBlock(pos, fluidState.createLegacyBlock(), 3);
        }

    }
    //return the amount by which we decay


}
