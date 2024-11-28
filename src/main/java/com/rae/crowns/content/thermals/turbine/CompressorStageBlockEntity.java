package com.rae.crowns.content.thermals.turbine;

import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompressorStageBlockEntity extends KineticBlockEntity implements IPressureChange {
    List<SteamCurrent> flows = List.of();
    float power;

    public CompressorStageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }
    @Override
    public float calculateStressApplied() {
        float combinedStress = getCombinedStress();
        this.lastStressApplied = combinedStress;
        return combinedStress;
    }

    private float getCombinedStress() {
        if (level == null) return 0;
        return -power/speed;// ? it's weird to do that but...
    }

    @Override
    public float pressureRatio() {
        //depend on speed ?
        return 2;
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
    }

}
