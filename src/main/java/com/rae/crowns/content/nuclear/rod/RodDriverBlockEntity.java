package com.rae.crowns.content.nuclear.rod;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NonnullDefault;


//TODO this was partly vibe coded, check the actual validity of the code : especially the pulling + the ticking

@NonnullDefault
public class RodDriverBlockEntity extends KineticBlockEntity implements IRodContainerBlockEntity {

    // Occupancy of the driver's *own* block - e.g. another machine on the far side feeding a rod
    // into this driver's body. Separate from the rod this driver itself is pushing outward.
    public    float  offset;//]-0.5, 0.5[
    protected double sequencedOffsetLimit;
    boolean running;
    private           float    clientOffsetDiff;
    private @Nullable RodBlock rodContained;
    private           float    cachedAbsorption;
    private           float    cachedModeration;
    private           float    cachedReflection;

    private           Direction facing = Direction.NORTH;
    private @Nullable BlockPos  tipPosition;// null if there is no rod near
    private           int       length = 0; // total length of the rod, forward AND backward of the

    public RodDriverBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        assert level != null;

        if (level.isClientSide) {
            clientOffsetDiff *= .75f;
        }

        offset += getMovementSpeed();
        updateNeutronProperties();

        if (!level.isClientSide) {

            float   movementSpeed = getMovementSpeed();
            boolean locked        = false;
            if (sequencedOffsetLimit > 0) {
                sequencedOffsetLimit = Math.max(0, sequencedOffsetLimit - Math.abs(movementSpeed));
                locked = sequencedOffsetLimit == 0;
            }

            if (locked) {
                running = false;
                sendData();
            } else {
                running = movementSpeed != 0;
            }

            if (!running)
                return;

            pull(movementSpeed);
            sendData();
        }
    }

    public float getMovementSpeed() {
        assert level != null;
        float movementSpeed = Mth.clamp(convertToLinear(getSpeed()), -.49f, .49f) + clientOffsetDiff / 2f;
        if (level.isClientSide)
            movementSpeed *= ServerSpeedProvider.get();
        if (sequencedOffsetLimit >= 0)
            movementSpeed = (float) Mth.clamp(movementSpeed, -sequencedOffsetLimit, sequencedOffsetLimit);
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

        cachedModeration = localModeration
                + insertedModeration;

        cachedAbsorption = localAbsorption
                + insertedAbsorption;

        cachedReflection = localReflection
                + insertedReflection;

        cachedModeration = Math.min(1f, cachedModeration);
        cachedAbsorption = Math.min(1f, cachedAbsorption);
        cachedReflection = Math.min(1f, cachedReflection);
    }

    /**
     * Pushes (positive) or pulls (negative) the rod by the given amount, in meters/blocks.
     * Extending can go through air, liquids, and any {@link IRodContainerBlockEntity}; it stops the
     * moment it hits anything else (collision). Retracting always succeeds, since it's only ever
     * un-doing ground this driver already covered.
     */
    private void pull(float meters) {
        assert level != null;
        if (meters == 0)
            return;

        facing = getBlockState().getValue(DirectionalBlock.FACING);
        boolean extending = meters > 0;
        float   remaining = Math.abs(meters);

        while (remaining > 0) {
            // an offset sitting exactly on a cell boundary belongs to whichever side we're
            // currently moving towards - re-base across it before doing anything else. Extending
            // never needs this: a commit always resets offset to 0, which is already a valid
            // starting point for filling the next cell forward.
            if (!extending && offset <= 0f) {
                if (tipPosition == null)
                    break; // fully retracted, nothing left to pull back
                shiftBack();
            }

            BlockPos leadingPos = tipPosition == null ? worldPosition.relative(facing) : tipPosition.relative(facing);

            if (extending && offset <= 0 && !canEnter(leadingPos))
                break; // collision: can't push into a solid, non-hollow obstruction

            float                    room   = extending ? 1f - offset : offset;
            float                    step   = Math.min(remaining, room);
            IRodContainerBlockEntity hollow = getHollow(leadingPos);

            if (hollow != null) {
                float applied = hollow.tryInsertRod(rod, facing.getOpposite(), offset);
                step = Math.abs(applied);
                if (extending && step <= 0)
                    break; // the neighbor has no room left for this rod right now
            }

            offset += extending ? step : -step;
            remaining -= step;

            if (extending && offset >= 1f) {
                offset = 0f;
                if (hollow == null)
                    placeRod(leadingPos);
                tipPosition = leadingPos;
            }
        }

        syncColumn();
    }

    @Override
    public Direction.Axis getAxis() {
        return facing.getAxis();
    }

    /**
     * Re-bases the leading edge backwards by one cell: frees up the cell currently at the tip
     * (draining it if it's a hollow neighbor, removing the placed RodBlock otherwise) and steps
     * the tip back towards the driver, leaving a full cell behind to keep draining from.
     */
    private void shiftBack() {
        assert tipPosition != null;
        IRodContainerBlockEntity hollow = getHollow(tipPosition);
        if (hollow != null) {
            //setOffset
            hollow.setRod(null);
            hollow.setOffset(0);
            hollow.tryInsertRod(null, facing.getOpposite(), -1f);
        }
        else
            removeRod(tipPosition);

        offset = 1f;
        BlockPos previous = tipPosition.relative(facing.getOpposite());
        tipPosition = previous.equals(worldPosition) ? null : previous;
    }

    private boolean canEnter(BlockPos pos) {
        assert level != null;
        if (getHollow(pos) != null)
            return true;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
    }

    private @Nullable IRodContainerBlockEntity getHollow(BlockPos pos) {
        assert level != null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IRodContainerBlockEntity hollow ? hollow : null;
    }

    private void placeRod(BlockPos pos) {
        assert level != null && rodContained != null;
        BlockState rodState = rodContained.defaultBlockState();
        if (rodState.hasProperty(RotatedPillarBlock.AXIS))
            rodState = rodState.setValue(RotatedPillarBlock.AXIS, facing.getAxis());
        level.setBlockAndUpdate(pos, rodState);
    }

    /**
     * Pushes the shared offset/speed out to every physically placed RodBlock in the column, on
     * both sides of the driver, so the whole rigid rod animates as one piece - including
     * whatever trails out the back as the front is retracted - instead of just the segment we
     * last touched. Also refreshes {@link #length} to the rod's total extent (forward + backward).
     */
    private void syncColumn() {
        float speed    = getMovementSpeed();
        int   forward  = syncDirection(facing, speed);
        int   backward = syncDirection(facing.getOpposite(), speed);
        length = forward + backward;
    }

    private void removeRod(BlockPos pos) {
        assert level != null;
        if (level.getBlockState(pos).getBlock() instanceof RodBlock)
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    /**
     * Walks outward from the driver in the given direction through matching RodBlock segments to
     * find the far end (no more rod, or it's a different rod crossing this one), then walks back
     * from there towards the driver applying the offset/speed update - the far end goes first
     * since that's where a collision would have been resolved already by {@link #pull} (only
     * relevant for the `facing` direction; the backward side is never blocked by anything we push
     * into), everything behind it just follows rigidly.
     *
     * @return how many segments make up the rod in this direction
     */
    private int syncDirection(Direction direction, float speed) {
        BlockPos cursor = worldPosition.relative(direction);
        BlockPos end    = null;
        int      count  = 0;
        while (isColumnSegment(cursor)) {
            end = cursor;
            count++;
            cursor = cursor.relative(direction);
        }

        Direction backwards = direction.getOpposite();
        BlockPos  pos       = end;
        for (int i = 0; i < count; i++) {
            if (level.getBlockEntity(pos) instanceof RodBlockEntity segment) {
                segment.setOffset(offset);
                segment.setSpeed(speed);
            }
            pos = pos.relative(backwards);
        }

        return count;
    }

    private boolean isColumnSegment(BlockPos pos) {
        assert level != null;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RodBlock
                && state.hasProperty(RotatedPillarBlock.AXIS)
                && state.getValue(RotatedPillarBlock.AXIS) == facing.getAxis();
    }


    @Override
    public void setRod(RodBlock rod) {
        this.rodContained = rod;
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        sequencedOffsetLimit = -1;

        if (sequenceContext != null && sequenceContext.instruction() == SequencerInstructions.TURN_DISTANCE)
            sequencedOffsetLimit = sequenceContext.getEffectiveValue(getTheoreticalSpeed());
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean("Running", running);
        compound.putFloat("Offset", offset);
        if (sequencedOffsetLimit >= 0)
            compound.putDouble("SequencedOffsetLimit", sequencedOffsetLimit);

        if (rodContained != null) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(rodContained);
            compound.putString("RodContained", key.toString());
        }
        if (tipPosition != null) {
            compound.putLong("TipPosition", tipPosition.asLong());
        }

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        float offsetBefore = offset;
        running = compound.getBoolean("Running");
        offset = compound.getFloat("Offset");
        sequencedOffsetLimit =
                compound.contains("SequencedOffsetLimit") ? compound.getDouble("SequencedOffsetLimit") : -1;

        if (compound.contains("RodContained")) {
            ResourceLocation key   = ResourceLocation.tryParse(compound.getString("RodContained"));
            Block            block = key == null ? null : BuiltInRegistries.BLOCK.get(key);
            rodContained = block instanceof RodBlock rod ? rod : null;
        } else {
            rodContained = null;
        }

        tipPosition = compound.contains("TipPosition")
                ? BlockPos.of(compound.getLong("TipPosition"))
                : null;

        if (clientPacket) {
            clientOffsetDiff = offset - offsetBefore;
            offset = offsetBefore;
        }

    }

    @Override
    protected boolean syncSequenceContext() {
        return true;
    }

    @Override
    public void setOffset(float offset) {
        this.offset = offset;
    }

    @Override
    public @Nullable RodBlock getBlockContained() {
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