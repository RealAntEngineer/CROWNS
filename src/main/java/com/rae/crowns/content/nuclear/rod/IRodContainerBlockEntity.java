package com.rae.crowns.content.nuclear.rod;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Interfaces for block able to hold a rod through them, the block entity will be responsible for storing what the rod is,
 * and it's position.
 */
public interface IRodContainerBlockEntity {

    /**
     * @return The Rod contained, null if none
     */
    @Nullable RodBlock getRodContained();

    Direction.Axis getAxis();

    /**
     *
     * @return the position of the rod contained [-0.5, 0.5]
     */
    float getOffset();

    void setOffset(float offset);

    /**
     *
     * @param rod The rod inserted
     * @param facing The face from which it's inserted
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
        if (direction == Direction.AxisDirection.NEGATIVE){
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