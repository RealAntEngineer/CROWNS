package com.rae.crowns.content.nuclear.rod;

import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NonnullDefault;

import java.util.List;


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
            offset += getMovementSpeed();
        }

        facing = getBlockState().getValue(RodDriverBlock.FACING);

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


        if (!level.isClientSide && (offset > 0.5f || offset < -0.5f))
            needsValidityCheck = true;

        if (needsValidityCheck) {
            needsValidityCheck = false;
            checkValidity(this);
        }

        updateNeutronProperties();
        sendData();

        if (justInserted) {
            justInserted = false;
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

    private void pull(float movementAmount) {
        assert level != null;
        float     oldOffset   = offset;
        Direction movementDir = movementAmount < 0 ? facing.getOpposite() : facing;

        BlockPos.MutableBlockPos tipPosition;
        Direction                syncDir;
        syncDir = movementDir.getOpposite();
        if (rodContained != null) {
            tipPosition = getBlockPos().mutable();

            int iterations = 0;
            while (iterations < 100) {
                BlockPos candidate = tipPosition.relative(movementDir); // does NOT mutate tipPosition
                if (notColumnSegment(candidate, oldOffset))
                    break;
                tipPosition.set(candidate); // only commit once validated
                iterations++;
            }

        } else {
            tipPosition = getBlockPos().relative(movementDir.getOpposite(), 1).mutable();

            if (level.getBlockEntity(tipPosition) instanceof IRodContainerBlockEntity rodContainerBE
                    && rodContainerBE.getRodContained() != null
                    && rodContainerBE.getAxis() == facing.getAxis()) {
                oldOffset = rodContainerBE.getOffset();
            } else {
                this.tipPosition = null;
                return;
            }

        }

        int iterations = 0;
        while (iterations < 100) {
            if (level.getBlockEntity(tipPosition) instanceof IRodContainerBlockEntity rodContainerBE) {
                rodContainerBE.setOffset(oldOffset + movementAmount);
                rodContainerBE.checkValidity((SmartBlockEntity) rodContainerBE);
            }

            if (notColumnSegment(tipPosition.move(syncDir, 1), oldOffset))
                break;

            iterations++;
        }

        this.tipPosition = tipPosition;
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

    private boolean notColumnSegment(BlockPos pos, float offset) {
        assert level != null;
        return !(level.getBlockEntity(pos) instanceof IRodContainerBlockEntity rodContainerBE) ||
                rodContainerBE.getRodContained() == null ||
                rodContainerBE.getAxis() != facing.getAxis() ||
                !(Math.abs(rodContainerBE.getOffset() - offset) < 0.1f);
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        sequencedOffsetLimit = -1;

        if (sequenceContext != null && sequenceContext.instruction() == SequencerInstructions.TURN_DISTANCE)
            sequencedOffsetLimit = sequenceContext.getEffectiveValue(getTheoreticalSpeed());
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putBoolean("Running", running);
        tag.putFloat("Offset", offset);
        if (sequencedOffsetLimit >= 0)
            tag.putDouble("SequencedOffsetLimit", sequencedOffsetLimit);

        if (rodContained != null) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(rodContained);
            tag.putString("RodContained", key.toString());
        }
        if (tipPosition != null) {
            tag.putLong("TipPosition", tipPosition.asLong());
        }

        super.write(tag, registries, clientPacket);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Running", running);
        tag.putFloat("Offset", offset);
        if (sequencedOffsetLimit >= 0)
            tag.putDouble("SequencedOffsetLimit", sequencedOffsetLimit);

        if (rodContained != null) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(rodContained);
            tag.putString("RodContained", key.toString());
        }
        if (tipPosition != null) {
            tag.putLong("TipPosition", tipPosition.asLong());
        }

        super.writeSafe(tag, registries);
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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        FormicApiLang.builder().text("rod contained : " + (rodContained == null ? "null" : rodContained.getDescriptionId()))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        FormicApiLang.builder().text("offset : " + offset)
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        FormicApiLang.builder().text("tip relative position : " + (tipPosition == null ? "no tip" : getBlockPos().subtract(tipPosition)))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        return true;
    }

    @Override
    protected boolean syncSequenceContext() {
        return true;
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
        justInserted = true;
    }

    public float getInterpolatedOffset(float partialTicks) {
        return offset + (rodContained != null ? (partialTicks - .5f) * getMovementSpeed() : 0);
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