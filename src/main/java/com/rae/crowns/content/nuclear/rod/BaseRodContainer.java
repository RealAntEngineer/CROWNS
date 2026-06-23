package com.rae.crowns.content.nuclear.rod;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BaseRodContainer extends SmartBlockEntity implements IRodContainerBlockEntity {

    public    float    offset;//]-0.5, 0.5[
    protected float    baseModeration;
    protected float    baseAbsorption;
    protected float    baseReflection;
    private   float    speed;
    private   float    clientOffsetDiff;
    private   RodBlock rodContained;
    private   float    cachedAbsorption;
    private   float    cachedModeration;
    private   float    cachedReflection;

    public BaseRodContainer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void setNeutronBaseProperties(float moderation, float absorption, float reflexion) {
        this.baseAbsorption = absorption;
        this.baseModeration = moderation;
        this.baseReflection = reflexion;
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

        offset += getMovementSpeed();
        updateNeutronProperties();
        sendData();//this will spam the network a bit. maybe it can be done once every few ticks ?
    }

    public float getMovementSpeed() {
        float movementSpeed = speed + clientOffsetDiff / 2f;
        assert level != null;
        if (level.isClientSide)
            movementSpeed *= ServerSpeedProvider.get();
        return movementSpeed;
    }

    private void updateNeutronProperties() {
        float occupiedLocal = rodContained == null ? 0 : (1f - Math.abs(offset));

        float occupiedInserted   = 0f;
        float insertedModeration = 0f;
        float insertedAbsorption = 0f;
        float insertedReflection = 0f;

        if (Math.abs(offset) > 0f) {
            int direction = offset < 0 ? 1 : -1;

            BlockPos neighbourPos = getBlockPos().relative(getAxis(), direction);
            assert level != null;
            BlockEntity be    = level.getBlockEntity(neighbourPos);
            BlockState  state = level.getBlockState(neighbourPos);

            if (be instanceof RodBlockEntity insertedRod
                    && state.getBlock() instanceof RodBlock insertedBlock) {

                occupiedInserted = direction < 0
                        ? Math.max(0f, insertedRod.getOffset())
                        : Math.max(0f, -insertedRod.getOffset());

                occupiedInserted = Math.min(occupiedInserted, 1f);

                insertedModeration =
                        occupiedInserted * insertedBlock.getMaterialModeration();

                insertedAbsorption =
                        occupiedInserted * insertedBlock.getMaterialAbsorption();

                insertedReflection =
                        occupiedInserted * insertedBlock.getMaterialReflection();
            }
        }

        float localModeration = 0f;
        float localAbsorption = 0f;
        float localReflection = 0f;

        if (rodContained != null) {
            localModeration =
                    occupiedLocal * rodContained.getMaterialModeration();

            localAbsorption =
                    occupiedLocal * rodContained.getMaterialAbsorption();

            localReflection =
                    occupiedLocal * rodContained.getMaterialReflection();
        }

        cachedModeration =
                baseModeration
                        + localModeration
                        + insertedModeration;

        cachedAbsorption =
                baseAbsorption
                        + localAbsorption
                        + insertedAbsorption;

        cachedReflection =
                baseReflection
                        + localReflection
                        + insertedReflection;

        cachedModeration = Math.min(1f, cachedModeration);
        cachedAbsorption = Math.min(1f, cachedAbsorption);
        cachedReflection = Math.min(1f, cachedReflection);
    }

    @Override
    public Direction.Axis getAxis() {
        return getBlockState().getValue(GraphiteSleeveBlock.AXIS);
    }

    @Override
    public void setRod(RodBlock rod) {
        rodContained = rod;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putFloat("Offset", offset);
        tag.putFloat("Speed", speed);
        if (rodContained != null) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(rodContained);
            tag.putString("RodContained", key.toString());
        }

        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        float offsetBefore = offset;
        offset = tag.getFloat("Offset");
        speed = tag.getFloat("Speed");
        if (tag.contains("RodContained")) {
            ResourceLocation key   = ResourceLocation.tryParse(tag.getString("RodContained"));
            Block            block = key == null ? null : BuiltInRegistries.BLOCK.get(key);
            rodContained = block instanceof RodBlock rod ? rod : null;
        } else {
            rodContained = null;
        }
        if (clientPacket) {
            clientOffsetDiff = offset - offsetBefore;
            offset = offsetBefore;
        }
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setOffset(float offset) {
        if (offset > 0.5)

        this.offset = offset;
        sendData();
    }

    @Override
    public RodBlock getBlockContained() {
        return rodContained;
    }

    @Override
    public float getOffset() {
        return offset;
    }

    public float getInterpolatedOffset(float partialTicks) {
        return offset + (partialTicks - .5f) * getMovementSpeed();
    }

    @Override
    public float getAbsorption() {
        return cachedAbsorption;
    }

    @Override
    public float getModeration() {
        return cachedModeration;
    }

    @Override
    public float getReflection() {
        return cachedReflection;
    }
}