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


//TODO this was partly vibe coded, check the actual validity of the code : especialy the pulling + the ticking

@NonnullDefault
public class RodDriverBlockEntity extends KineticBlockEntity implements IRodContainerBlockEntity {

    // --- The rod this driver pushes/pulls through the channel ahead of it ---
    // tipPosition describes the last *fully committed* cell ahead of the driver (in `facing`):
    // either a physically placed RodBlock (open air/liquid path), or a hollow neighbour we've
    // fully loaded to 1m. offset is the partial fill ([0,1)) of the cell immediately beyond the
    // tip - the only place along the rod that can ever be "in between".
    private @Nullable Direction facing;     // null if nothing is loaded
    private @Nullable RodBlock  rodType;    // which rod this driver is driving, null if none loaded
    private @Nullable BlockPos  tipPosition;// null if the rod hasn't left the driver's mouth yet
    private           int       length = 0; // total length of the rod, forward AND backward of the
    // driver - purely informational, recomputed lazily by
    // syncColumn() each pull(), not used to drive movement

    public    float   offset; // position of the rod [-0.5, -0.5]
    public    boolean running;

    protected double sequencedOffsetLimit;
    // Custom position sync
    protected float  clientOffsetDiff;

    // Occupancy of the driver's *own* block - e.g. another machine on the far side feeding a rod
    // into this driver's body. Separate from the rod this driver itself is pushing outward.
    private final RodOccupancy selfOccupancy = new RodOccupancy();

    public RodDriverBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    /**
     * Designates which rod this driver pushes, and in which direction it pushes it. Call this
     * once when the channel is set up. Nothing in the files I was given wires this up (e.g. from
     * block placement, or from whatever first feeds a rod into the driver) - that's on you.
     */
    public void loadRod(Direction facing, RodBlock rodType) {
        this.facing = facing;
        this.rodType = rodType;
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        assert level != null;

        if (facing == null) {
            facing = getBlockState().getValue(DirectionalBlock.FACING);
        }

        if (level.isClientSide) {
            clientOffsetDiff *= .75f;
            return;
        }


        if (!running)
            return;

        float movementSpeed = getMovementSpeed();
        if (movementSpeed == 0)
            return;

        boolean locked = false;
        if (sequencedOffsetLimit > 0) {
            sequencedOffsetLimit = Math.max(0, sequencedOffsetLimit - Math.abs(movementSpeed));
            locked = sequencedOffsetLimit == 0;
        }

        pull(movementSpeed);
        sendData();

        if (locked) {
            running = false;
            sendData();
        }
    }

    /**
     * Pushes (positive) or pulls (negative) the rod by the given amount, in meters/blocks.
     * Extending can go through air, liquids, and any {@link IRodContainerBlockEntity}; it stops the
     * moment it hits anything else (collision). Retracting always succeeds, since it's only ever
     * un-doing ground this driver already covered.
     */
    private void pull(float meters) {
        assert level != null;
        if (facing == null || rodType == null || meters == 0)
            return;

        boolean extending = meters > 0;
        float remaining = Math.abs(meters);

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

            float room = extending ? 1f - offset : offset;
            float                    step   = Math.min(remaining, room);
            IRodContainerBlockEntity hollow = getHollow(leadingPos);

            if (hollow != null) {
                float applied = hollow.canInsertRod(facing.getOpposite(), extending ? step : -step);
                step = Math.abs(applied);
                if (extending && step <= 0)
                    break; // the neighbour has no room left for this rod right now
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

    /**
     * Re-bases the leading edge backwards by one cell: frees up the cell currently at the tip
     * (draining it if it's a hollow neighbour, removing the placed RodBlock otherwise) and steps
     * the tip back towards the driver, leaving a full cell behind to keep draining from.
     */
    private void shiftBack() {
        assert facing != null && rodType != null && tipPosition != null;
        IRodContainerBlockEntity hollow = getHollow(tipPosition);
        if (hollow != null)
            hollow.canInsertRod(facing.getOpposite(), -1f);
        else
            removeRod(tipPosition);

        offset = 1f;
        BlockPos previous = tipPosition.relative(facing.getOpposite());
        tipPosition = previous.equals(worldPosition) ? null : previous;
    }

    /**
     * Pushes the shared offset/speed out to every physically placed RodBlock in the column, on
     * both sides of the driver, so the whole rigid rod animates as one piece - including
     * whatever trails out the back as the front is retracted - instead of just the segment we
     * last touched. Also refreshes {@link #length} to the rod's total extent (forward + backward).
     */
    private void syncColumn() {
        assert facing != null;
        float speed = getMovementSpeed();
        int forward = syncDirection(facing, speed);
        int backward = syncDirection(facing.getOpposite(), speed);
        length = forward + backward;
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
        BlockPos end = null;
        int count = 0;
        while (isColumnSegment(cursor)) {
            end = cursor;
            count++;
            cursor = cursor.relative(direction);
        }

        Direction backwards = direction.getOpposite();
        BlockPos pos = end;
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
        assert level != null && facing != null;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RodBlock
                && state.hasProperty(RotatedPillarBlock.AXIS)
                && state.getValue(RotatedPillarBlock.AXIS) == facing.getAxis();
    }

    private @Nullable IRodContainerBlockEntity getHollow(BlockPos pos) {
        assert level != null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IRodContainerBlockEntity hollow ? hollow : null;
    }

    private boolean canEnter(BlockPos pos) {
        assert level != null;
        if (getHollow(pos) != null)
            return true;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
    }

    private void placeRod(BlockPos pos) {
        assert level != null && facing != null && rodType != null;
        BlockState rodState = rodType.defaultBlockState();
        if (rodState.hasProperty(RotatedPillarBlock.AXIS))
            rodState = rodState.setValue(RotatedPillarBlock.AXIS, facing.getAxis());
        level.setBlockAndUpdate(pos, rodState);
    }

    private void removeRod(BlockPos pos) {
        assert level != null;
        if (level.getBlockState(pos).getBlock() instanceof RodBlock)
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    public float getInterpolatedOffset(float partialTicks) {
        return offset + (partialTicks - .5f) * getMovementSpeed();
    }

    public float getMovementSpeed() {
        assert level !=null;
        float movementSpeed = Mth.clamp(convertToLinear(getSpeed()), -.49f, .49f) + clientOffsetDiff / 2f;
        if (level.isClientSide)
            movementSpeed *= ServerSpeedProvider.get();
        if (sequencedOffsetLimit >= 0)
            movementSpeed = (float) Mth.clamp(movementSpeed, -sequencedOffsetLimit, sequencedOffsetLimit);
        return movementSpeed;
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

        if (facing != null)
            compound.putString("Facing", facing.getName());
        if (rodType != null) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(rodType);
            compound.putString("RodType", key.toString());
        }
        if (tipPosition != null) {
            compound.putInt("TipX", tipPosition.getX());
            compound.putInt("TipY", tipPosition.getY());
            compound.putInt("TipZ", tipPosition.getZ());
        }

        selfOccupancy.write(compound);

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        float offsetBefore  = offset;
        running = compound.getBoolean("Running");
        offset = compound.getFloat("Offset");
        sequencedOffsetLimit =
                compound.contains("SequencedOffsetLimit") ? compound.getDouble("SequencedOffsetLimit") : -1;

        facing = compound.contains("Facing") ? Direction.byName(compound.getString("Facing")) : null;

        if (compound.contains("RodType")) {
            ResourceLocation key = ResourceLocation.tryParse(compound.getString("RodType"));
            Block block = key == null ? null : BuiltInRegistries.BLOCK.get(key);
            rodType = block instanceof RodBlock rod ? rod : null;
        } else {
            rodType = null;
        }

        tipPosition = compound.contains("TipX")
                ? new BlockPos(compound.getInt("TipX"), compound.getInt("TipY"), compound.getInt("TipZ"))
                : null;

        selfOccupancy.read(compound);

        super.read(compound, registries, clientPacket);

        if (clientPacket && running) {
            clientOffsetDiff = offset - offsetBefore;
            offset = offsetBefore;
        }
    }

    @Override
    protected boolean syncSequenceContext() {
        return true;
    }

    @Override
    public float insertRod(RodBlock block, Direction facing, float amount) {
        float applied = selfOccupancy.insert(block, amount);
        if (applied != 0)
            notifyUpdate();
        return applied;
    }

    @Override
    public float getAbsorption() {
        return selfOccupancy.getAbsorption();
    }

    @Override
    public float getModeration() {
        return selfOccupancy.getModeration();
    }

    @Override
    public float getReflection() {
        return selfOccupancy.getReflection();
    }
}