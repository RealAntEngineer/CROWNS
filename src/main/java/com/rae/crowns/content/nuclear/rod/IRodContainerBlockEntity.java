package com.rae.crowns.content.nuclear.rod;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * Interfaces for block able to hold a rod through them, the block entity will be responsible for storing what the rod is,
 * and it's position.
 */
public interface IRodContainerBlockEntity {

    /**
     * Resolves at most one hand-off to the neighboring container when this rod's
     * offset has left [-0.5, 0.5]. Any further cascade
     * (e.g. a row of touching rods) is picked up by the neighbor on its own next
     * tick, not synchronously in this call.
     */
    default void checkValidity(SmartBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        assert level != null;
        if (level.isClientSide) return; // block placement must stay server-authoritative
        if (getRodContained() == null) return;
        float    offset      = getOffset();
        int      relativePos = offset > 0 ? 1 : -1;
        BlockPos pos         = blockEntity.getBlockPos().relative(getAxis(), relativePos);
        Direction facing = Direction.get(
                offset < 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE,
                getAxis());

        if (level.getBlockEntity(pos) instanceof IRodContainerBlockEntity neighbour) {
            InsertionResult result = neighbour.tryInsertRod(getRodContained(), facing, offset);
            if (result.removeBlock()) {//collision should be
                neighbour.setRod(getRodContained());
                neighbour.setOffset(result.offset() - relativePos);
                setRod(null);
                //this.setOffset(0); please don't.
            } else {
                this.setOffset(result.offset());
            }
        } else if (level.getBlockState(pos).canBeReplaced()) {
            if (offset <= 0.5f && offset >= -0.5f) return;//if it doesn't need to move don't move it
            FluidState state       = level.getFluidState(pos);
            boolean    waterlogged = state.is(FluidTags.WATER);
            level.setBlock(pos, getRodContained().defaultBlockState()
                            .trySetValue(RodBlock.AXIS, getAxis())
                            .trySetValue(RodBlock.WATERLOGGED, waterlogged)
                    , 11);
            if (level.getBlockEntity(pos) instanceof IRodContainerBlockEntity neighbour) {
                neighbour.setRod(getRodContained());
                neighbour.setOffset(offset - relativePos);
            }
            setRod(null);
            this.setOffset(0);
        } else {
            // blocked by a solid, non-container block.
            this.setOffset(0);
        }
        blockEntity.sendData();
    }

    /**
     * @return The Rod contained, null if none
     */
    @Nullable RodBlock getRodContained();

    /**
     *
     * @return the position of the rod contained [-0.5, 0.5]
     */
    float getOffset();

    Direction.Axis getAxis();

    /**
     *
     * @param rod       The rod inserted
     * @param facing    The face from which it's inserted
     * @param newOffset The offset relative to the Block inserting
     * @return The result
     */
    default InsertionResult tryInsertRod(RodBlock rod, Direction facing, float newOffset) {
        if (getRodContained() == null) {
            boolean shouldRemoveBlock = newOffset > 0.5 || newOffset < -0.5;
            if (shouldRemoveBlock) setRod(rod);
            return new InsertionResult(shouldRemoveBlock, newOffset);
        }

        Direction.AxisDirection direction = facing.getAxisDirection();
        Direction.Axis          axis      = facing.getAxis();
        if (axis != getAxis())
            return new InsertionResult(false, 0);

        //First clamp to the maximum
        float clampedOffset;
        if (direction == Direction.AxisDirection.NEGATIVE) {
            clampedOffset = Math.min(getOffset(), newOffset);
        } else {
            clampedOffset = Math.max(getOffset(), newOffset);
        }

        if (clampedOffset > 0.5 || clampedOffset < -0.5) {
            return new InsertionResult(true, clampedOffset);
        } else {
            return new InsertionResult(false, clampedOffset);
        }
    }

    void setRod(RodBlock rod);

    void setOffset(float offset);

    float getInterpolatedOffset(float partialTicks);

    /**
     *
     * @return the percentage of incoming radiation moderated
     */
    float getAbsorption();

    /**
     *
     * @return the percentage of incoming radiation moderated
     */
    float getModeration();

    /**
     * unused, for future radiation computation.
     *
     * @return the percentage of incoming radiation reflected
     */
    float getReflection();

    record InsertionResult(boolean removeBlock, float offset) {

    }
}