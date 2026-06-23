package com.rae.crowns.content.nuclear.rod;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class GraphiteSleeveBlockEntity extends SmartBlockEntity implements IRodContainerBlockEntity {
    // Graphite itself is a decent neutron moderator and a poor absorber/reflector - this is the
    // sleeve's own contribution, on top of whatever rod(s) currently occupy it.
    // TODO placeholder values, tune for balance.
    private static final float BASE_MODERATION = 0.6f;
    private static final float BASE_ABSORPTION = 0f;
    private static final float BASE_REFLECTION = 0f;

    // Tracks which rod(s) are currently threaded through this sleeve and how much of each
    // (at most 1m total, since the sleeve itself is 1m long).
    private final RodOccupancy occupancy = new RodOccupancy();

    public GraphiteSleeveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public float insertRod(RodBlock block, Direction facing, float amount) {
        float applied = occupancy.insert(block, amount);
        if (applied != 0)
            notifyUpdate();
        return applied;
    }

    @Override
    public float getAbsorption() {
        float occupied = occupancy.getTotalOccupied();
        return BASE_ABSORPTION * (1 - occupied) + occupancy.getAbsorption();
    }

    @Override
    public float getModeration() {
        float occupied = occupancy.getTotalOccupied();
        return BASE_MODERATION * (1 - occupied) + occupancy.getModeration();
    }

    @Override
    public float getReflection() {
        float occupied = occupancy.getTotalOccupied();
        return BASE_REFLECTION * (1 - occupied) + occupancy.getReflection();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        occupancy.write(tag);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        occupancy.read(tag);
    }
}