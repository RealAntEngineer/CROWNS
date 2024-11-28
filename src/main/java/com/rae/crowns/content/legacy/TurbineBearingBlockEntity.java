package com.rae.crowns.content.legacy;

import com.rae.crowns.api.flow.client.FlowParticleData;
import com.rae.crowns.api.flow.commun.FlowLine;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TurbineBearingBlockEntity extends CROWNSBearingBlockEntity {

    protected ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    protected float lastGeneratedSpeed;
    protected boolean queuedReassembly;

    public TurbineBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    @Override
    public void updateGeneratedRotation() {
        super.updateGeneratedRotation();
        lastGeneratedSpeed = getGeneratedSpeed();
        queuedReassembly = false;
    }
    @Override
    public void onSpeedChanged(float prevSpeed) {
        boolean cancelAssembly = assembleNextTick;
        super.onSpeedChanged(prevSpeed);
        assembleNextTick = cancelAssembly;
    }

    @Override
    public void tick() {
        super.tick();
        spawnParticles();
        if (level.isClientSide())
            return;
        if (running){
            updateGeneratedRotation();
        }
        if (!queuedReassembly)
            return;
        queuedReassembly = false;
        if (!running)
            assembleNextTick = true;
    }

    private void spawnParticles() {
        Direction facing = getBlockState().getValue(TurbineBearingBlock.FACING);
        if (level != null && level.isClientSide) {
            if (movedContraption != null) {
                if (movedContraption.getContraption() instanceof TurbineContraption turbineContraption) {
                    List<Long> steamCurrentsPos = turbineContraption.getCurrents();
                    for (Long steamPos : steamCurrentsPos){
                        float offset = 0.5f;
                        Vec3 turbinePos = VecHelper.getCenterOf(getBlockPos())
                            .add(Vec3.atLowerCornerOf(facing.getNormal())
                                    .scale(offset));
                        Vec3 steamPosVec = VecHelper.getCenterOf(BlockPos.of(steamPos))
                                .add(Vec3.atLowerCornerOf(facing.getNormal())
                                        .scale(offset));
                        //Vec3 offsetVec = steamPosVec.subtract(turbinePos);

                        if (level.random.nextFloat() < AllConfigs.client().fanParticleDensity.get()) {

                            FlowLine spline = new FlowLine(
                                    List.of(steamPosVec.relative(facing,0.5),steamPosVec.relative(facing,3),
                                            steamPosVec.relative(facing,4).relative(Direction.UP,2),
                                            steamPosVec.relative(facing,6).relative(Direction.UP,2).relative(Direction.SOUTH,3)
                                    ),
                                    List.of(0.01),
                                    List.of(Color.WHITE));
                            level.addParticle(new FlowParticleData(spline,0),  steamPosVec.x, steamPosVec.y, steamPosVec.z, 0, 0, 0);
                            spline.getControlPoints().forEach(
                                    p ->
                                    level.addParticle(ParticleTypes.BUBBLE,p.x,p.y,p.z,0,0,0)
                            );
                            //level.addParticle(new SteamFlowParticleData(Vec3.atLowerCornerOf(facing.getNormal()), offsetVec), steamPosVec.x, steamPosVec.y, steamPosVec.z, 0, 0, 0);

                        }
                    }
                }
            }
        }
    }

    /*@Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide())
            return;
        if (running){
            updateGeneratedRotation();
        }
    }*/
    private float getCombinedCapacity() {
        if (movedContraption == null) return 0;
        return (float) ((float) ((TurbineContraption) movedContraption.getContraption()).getBladeBlocks() * BlockStressValues.getCapacity(getStressConfigKey()));
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = getCombinedCapacity();
        this.lastCapacityProvided = capacity;
        return capacity;
    }
    public void disassembleForMovement() {
        if (!running)
            return;
        disassemble();
        queuedReassembly = true;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!running)
            return 0;
        if (movedContraption == null)
            return lastGeneratedSpeed;
        int blades = ((TurbineContraption) movedContraption.getContraption()).getBladeBlocks();
        int nbrOfSources = ((TurbineContraption) movedContraption.getContraption()).getCurrents().size();
        return Mth.clamp(nbrOfSources*blades, 0, 256) * getAngleSpeedDirection();
    }

    @Override
    protected void notifyStressCapacityChange(float capacity) {
        super.notifyStressCapacityChange(capacity);
    }

    @Override
    protected boolean isTurbine() {
        return true;
    }

    protected float getAngleSpeedDirection() {
        WindmillBearingBlockEntity.RotationDirection rotationDirection = WindmillBearingBlockEntity.RotationDirection.values()[movementDirection.getValue()];
        return (rotationDirection == WindmillBearingBlockEntity.RotationDirection.CLOCKWISE ? 1 : -1);
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("LastGenerated", lastGeneratedSpeed);
        compound.putBoolean("QueueAssembly", queuedReassembly);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        if (!wasMoved)
            lastGeneratedSpeed = compound.getFloat("LastGenerated");
        queuedReassembly = compound.getBoolean("QueueAssembly");
        super.read(compound, clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.remove(movementMode);
        movementDirection = new ScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class,
                Lang.translateDirect("contraptions.windmill.rotation_direction"), this, getMovementModeSlot());
        movementDirection.withCallback($ -> onDirectionChanged());
        behaviours.add(movementDirection);
        registerAwardables(behaviours, AllAdvancements.WINDMILL, AllAdvancements.WINDMILL_MAXED);
    }

    private void onDirectionChanged() {
        if (!running)
            return;
        if (!level.isClientSide)
            updateGeneratedRotation();
    }

    @Override
    public boolean isWoodenTop() {
        return true;
    }

    @Override
    public void remove() {
        super.remove();
    }


}
