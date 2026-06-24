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
    protected RodBlock rodContained;
    private   float    cachedAbsorption;
    private   float    cachedModeration;
    private   float    cachedReflection;
    private boolean justInserted;
    private boolean needsValidityCheck;


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

    //int tickCount = 3;
    @Override
    public void tick() {
        super.tick();

        assert level != null;

        if (level.isClientSide)
            clientOffsetDiff *= .75f;

        offset += getMovementSpeed();

        if (!level.isClientSide && (offset > 0.5f || offset < -0.5f))
            needsValidityCheck = true;

        if (needsValidityCheck)
            checkValidity();

        updateNeutronProperties();
        sendData();//this will spam the network a bit. maybe it can be done once every few ticks ?
        if (justInserted){
            justInserted = false;
        }
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
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putFloat("Offset", offset);
        tag.putFloat("Speed", speed);
        tag.putBoolean("JustInserted", justInserted);
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
        justInserted = tag.getBoolean("JustInserted");
        if (tag.contains("RodContained")) {
            ResourceLocation key   = ResourceLocation.tryParse(tag.getString("RodContained"));
            Block            block = key == null ? null : BuiltInRegistries.BLOCK.get(key);
            rodContained = block instanceof RodBlock rod ? rod : null;
        } else {
            rodContained = null;
        }
        if (clientPacket && !justInserted) {
            clientOffsetDiff = offset - offsetBefore;
            offset = offsetBefore;
        } else {
            clientOffsetDiff = 0;
        }
        justInserted = false;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public RodBlock getBlockContained() {
        return rodContained;
    }

    @Override
    public Direction.Axis getAxis() {
        return getBlockState().getValue(GraphiteSleeveBlock.AXIS);
    }

    @Override
    public float getOffset() {
        return offset;
    }

    /**
     * Resolves at most one hand-off to the neighboring container when this rod's
     * offset has left [-0.5, 0.5]. Any further cascade
     * (e.g. a row of touching rods) is picked up by the neighbor on its own next
     * tick, not synchronously in this call.
     */
    private void checkValidity() {
        needsValidityCheck = false;
        assert level != null;
        if (level.isClientSide) return; // block placement must stay server-authoritative
        if (rodContained == null) return;

        int relativePos = offset > 0 ? 1 : -1;
        BlockPos pos = getBlockPos().relative(getAxis(), relativePos);
        Direction facing = Direction.get(
                offset < 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE,
                getAxis());

        if (level.getBlockEntity(pos) instanceof IRodContainerBlockEntity neighbour) {
            InsertionResult result = neighbour.tryInsertRod(rodContained, facing, offset);
            if (result.removeBlock()) {//collision should be
                neighbour.setRod(rodContained);
                neighbour.setOffset(result.offset() - relativePos);
                setRod(null);
                this.offset = 0;
            } else {
                this.offset = result.offset();
            }
        } else if (level.getBlockState(pos).isAir()) {
            if (offset <= 0.5f && offset >= -0.5f) return;//if it doesn't need to move don't move it
            level.setBlock(pos, rodContained.defaultBlockState().setValue(RodBlock.AXIS, getAxis()), 11);
            if (level.getBlockEntity(pos) instanceof IRodContainerBlockEntity neighbour) {
                neighbour.setRod(rodContained);
                neighbour.setOffset(offset - relativePos);
            }
            setRod(null);
            this.offset = 0;
        } else {
            // blocked by a solid, non-container block.
            this.offset = 0;
        }

        sendData();
    }

    @Override
    public void setOffset(float offset) {
        if (offset != this.offset)
            needsValidityCheck = true;
        this.offset = offset;
        sendData();
    }

    @Override
    public void setRod(RodBlock rod) {
        rodContained = rod;
        justInserted = true;
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