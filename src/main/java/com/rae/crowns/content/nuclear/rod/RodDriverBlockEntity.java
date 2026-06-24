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
    private           float     clientOffsetDiff;
    private @Nullable RodBlock  rodContained;
    private           float     cachedAbsorption;
    private           float     cachedModeration;
    private           float     cachedReflection;
    private           boolean   justInserted;
    private           boolean   needsValidityCheck;
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
        if (!level.isClientSide && (offset > 0.5f || offset < -0.5f))
            needsValidityCheck = true;

        if (needsValidityCheck)
            checkValidity();

        updateNeutronProperties();
        sendData();

        if (justInserted) {
            justInserted = false;
        }

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

    private void pull(float movementAmount){
        assert level != null;
        //find the tip
        Direction movementDir;
        if (movementAmount < 0){
            movementDir = facing.getOpposite();
        } else {
            movementDir = facing;
        }

        int maxIteration = 0;
        BlockPos.MutableBlockPos tipPosition = getBlockPos().relative(movementDir, -1).mutable();
        //find tip.
        while (maxIteration < 100){
            if (!isColumnSegment(tipPosition.relative(movementDir, 1))){
                break;
            }
            maxIteration++;

        }
        //it means That we failed to find a rod.
        if (maxIteration ==0) {
            return;
        }
        maxIteration = 0;
        while (maxIteration < 100) {

            if (level.getBlockEntity(tipPosition) instanceof IRodContainerBlockEntity rodContainerBE) {
                //tryInsertRod(rodContainerBE.getRodContained(), movementDir.getOpposite(), offset);
                rodContainerBE.setOffset(offset);
            }
            if (!isColumnSegment(tipPosition.relative(movementDir, 1))){
                break;
            }

            maxIteration++;
        }
        //get direction -> if it's different drop everything.
        //tipPosition

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
        justInserted = compound.getBoolean("JustInserted");
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

        if (clientPacket && !justInserted) {
            clientOffsetDiff = offset - offsetBefore;
            offset = offsetBefore;
        } else {
            clientOffsetDiff = 0;
        }
        justInserted = false;

    }

    @Override
    protected boolean syncSequenceContext() {
        return true;
    }

    /**
     * Resolves at most one hand-off to the neighboring container when this rod's
     * offset has left [-0.5, 0.5]. Any further cascade
     * (e.g. a row of touching rods) is picked up by the neighbor on its own next
     * tick, not synchronously in this call.
     */
    private void checkValidity() {
        assert level != null;
        if (level.isClientSide) return; // block placement must stay server-authoritative
        if (rodContained == null) return;
        float    offset      = getOffset();
        int      relativePos = offset > 0 ? 1 : -1;
        BlockPos pos         = getBlockPos().relative(getAxis(), relativePos);
        Direction facing = Direction.get(
                offset < 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE,
                getAxis());

        if (level.getBlockEntity(pos) instanceof IRodContainerBlockEntity neighbour) {
            InsertionResult result = neighbour.tryInsertRod(rodContained, facing, offset);
            if (result.removeBlock()) {//collision should be
                neighbour.setRod(rodContained);
                neighbour.setOffset(result.offset() - relativePos);
                setRod(null);
                this.setOffset(0);
            } else {
                this.setOffset(result.offset());
            }
        } else if (level.getBlockState(pos).isAir()) {
            if (offset <= 0.5f && offset >= -0.5f) return;//if it doesn't need to move don't move it
            level.setBlock(pos, rodContained.defaultBlockState().setValue(RodBlock.AXIS, getAxis()), 11);
            if (level.getBlockEntity(pos) instanceof IRodContainerBlockEntity neighbour) {
                neighbour.setRod(rodContained);
                neighbour.setOffset(offset - relativePos);
            }
            setRod(null);
            this.setOffset(0);
        } else {
            // blocked by a solid, non-container block.
            this.setOffset(0);
        }
        needsValidityCheck = false;
        sendData();
    }

    @Override
    public @Nullable RodBlock getRodContained() {
        return rodContained;
    }

    @Override
    public Direction.Axis getAxis() {
        return facing.getAxis();
    }

    @Override
    public float getOffset() {
        return offset;
    }

    @Override
    public void setOffset(float offset) {
        if (offset != this.offset)
            needsValidityCheck = true;
        this.offset = offset;
        sendData();
    }

    @Override
    public void setRod(@Nullable RodBlock rod) {
        this.rodContained = rod;
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

    private boolean isColumnSegment(BlockPos pos) {
        assert level != null;
        return level.getBlockEntity(pos) instanceof IRodContainerBlockEntity rodContainerBE &&
                rodContainerBE.getRodContained() != null &&
                rodContainerBE.getAxis() == facing.getAxis() &&
                rodContainerBE.getOffset() == this.getOffset();
    }
}