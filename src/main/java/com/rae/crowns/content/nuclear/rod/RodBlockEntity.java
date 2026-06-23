package com.rae.crowns.content.nuclear.rod;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RodBlockEntity extends SmartBlockEntity {
    //TODO hold a float offset

    public  float offset;//]-1, 1[
    private float speed;
    private float clientOffsetDiff;

    public RodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();

        assert level != null;

        if (level.isClientSide)
            clientOffsetDiff *= .75f;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putFloat("offset", offset);
        tag.putFloat("speed", speed);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        float offsetBefore  = offset;
        offset = tag.getFloat("offset");
        speed = tag.getFloat("speed");

        if (clientPacket) {
            clientOffsetDiff = offset - offsetBefore;
            offset = offsetBefore;
        }
    }


    public float getInterpolatedOffset(float partialTicks) {
        return offset + (partialTicks - .5f) * getMovementSpeed();
    }

    public void setSpeed(float speed){
        this.speed = speed;
    }

    public void setOffset(float offset){
        this.offset = offset;
    }

    public float getMovementSpeed() {
        float movementSpeed = speed + clientOffsetDiff / 2f;
        assert level != null;
        if (level.isClientSide)
            movementSpeed *= ServerSpeedProvider.get();
        return movementSpeed;
    }
}