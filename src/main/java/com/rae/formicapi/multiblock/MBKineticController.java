package com.rae.formicapi.multiblock;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * the main block for the multiblock, this the block that is used for the model, to make the model work you will need
 * to look at the default offset given in the MBShape for the size chosen
 */
public abstract class MBKineticController extends DirectionalKineticBlock implements IMBController {
    final MBStructureBlock structure;
    protected MBKineticController(Properties properties, MBStructureBlock structure) {
        super(properties);
        this.structure = structure;
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    @Override
    public void setPlacedBy(@NotNull Level worldIn, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, entity, stack);
        repairStructure(worldIn, pos, state.getValue(FACING));
    }
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(FACING,context.getClickedFace());

    }

    public MBStructureBlock getStructure() {
        return structure;
    }
}