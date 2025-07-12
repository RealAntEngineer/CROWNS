package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.config.Configs;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class TurbineStageBlockEntity extends GeneratingKineticBlockEntity implements ISteamPressureChange {
    //the turbine add itself to the SteamCurrent
    protected List<SteamCurrent> flows = List.of();
    float power;
    public int initialTicks;
    public TurbineStageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
        initialTicks = 3;
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public boolean isSource() {
        return true;
    }

    @Override
    public float getGeneratedSpeed() {
        //if flows is empty and power!=0 it means that the BE is being loaded, we need to trust only the power in that case
        //so there is no need to check for the flows.
        return power==0?0: Configs.SERVER.kinetics.turbineSpeed.get(); // * direction du flux
    }
    @Override
    public float calculateAddedStressCapacity() {//it's the stress base not the real stress
        float capacity = (float) (getCombinedCapacity() * Configs.SERVER.kinetics.turbineCoefficient.get());
        this.lastCapacityProvided = capacity;
        return capacity;
    }
    private float getCombinedCapacity() {
        if (level == null) return 0;

        return getGeneratedSpeed()==0?power:power/getGeneratedSpeed();// capacity is
    }

    @Override
    public float pressureRatio() {
        return 0.5f;
    }

    @Override
    public void tick() {
        super.tick();

    }

    @Override
    public void lazyTick() {
        assert level!=null;
        //update the List of currents
        AABB bound = new AABB(worldPosition);
        List<Direction.Axis> plane = Arrays.stream(Direction.Axis.values()).filter(
                (axis -> !axis.test(getBlockState().getValue(TurbineStageBlock.FACING)))).toList();
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.NEGATIVE,plane.get(0))
                .getNormal()));
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE,plane.get(0))
                .getNormal()));
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.NEGATIVE,plane.get(1))
                .getNormal()));
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE,plane.get(1))
                .getNormal()));

        flows = level.getEntitiesOfClass(SteamCurrent.class, bound);
        power = 0;
        flows.forEach(f -> power += f.getPowerForStage(this));

        if (level.isClientSide()) return;
        updateGeneratedRotation();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("power", power);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        power = compound.getFloat("power");
    }
}
