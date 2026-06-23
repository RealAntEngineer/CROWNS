package com.rae.crowns.content.nuclear.rod;

import net.minecraft.core.Direction;

/**
 * Interfaces for block able to hold a rod through them, the block entity will be responsible for storing what the rod is,
 * and it's position.
 */
public interface IRodContainerBlockEntity {

    /**
     * insert a new rod into the block at the specified offset
     * @param facing : The face from which it's inserted
     * @param offset : The position relative to us, it's trying to reach
     * @return true if it managed to insert the rod, false if it failed (collided)
     */
    default boolean canInsertRod(Direction facing, float offset) {
        //TODO maybe it's better if the offset reference is in the block from which we insert
        if (getBlockContained() == null) return true;
        Direction.AxisDirection direction = facing.getAxisDirection();
        Direction.Axis axis = facing.getAxis();
        if (axis != getAxis()) return false;

        return direction == Direction.AxisDirection.NEGATIVE ? (getOffset() - 1) > offset : (getOffset() + 1) < offset;

    }

    Direction.Axis getAxis();

    boolean tryInsertRod(RodBlock block, Direction.Axis axis, float newOffset);

    void setRod(RodBlock rod);

    void setOffset(float offset);
    /**
     * @return The Rod contained, null if none
     */
    RodBlock getBlockContained();

    /**
     *
     * @return the position of the rod contained [-0.5, 0.5]
     */
    float getOffset();
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
     * @return the percentage of incoming radiation reflected
     */
    float getReflection();
}