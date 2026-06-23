package com.rae.crowns.content.nuclear.rod;

import net.minecraft.core.Direction;

/**
 * Interfaces for block able to hold a rod through them, the block entity will be responsible for storing what the rod is,
 * and it's position.
 */
public interface IHollowBlockEntity {

    /**
     * insert a new rod into the block by the given amount.
     * @param block  : The rod inserted
     * @param facing : The face from which it's inserted
     * @param amount : The amount inserted (in meter/block), negative amount are counted as removal
     * @return the amount actually applied, after clamping to whatever capacity is left (e.g. the block
     *         is already full, or already hosts two distinct rods). Callers should treat a returned
     *         value smaller than the requested amount as a sign they can't push any further.
     */
    float insertRod(RodBlock block, Direction facing, float amount);

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