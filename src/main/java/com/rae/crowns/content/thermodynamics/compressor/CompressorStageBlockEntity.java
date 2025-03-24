package com.rae.crowns.content.thermodynamics.compressor;

import com.rae.crowns.content.thermodynamics.turbine.ISteamPressureChange;
import com.rae.crowns.content.thermodynamics.turbine.SteamCurrent;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class CompressorStageBlockEntity extends KineticBlockEntity {
    float power;

    public CompressorStageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }
    @Override
    public float calculateStressApplied() {
        float combinedStress = getCombinedStress();
        this.lastStressApplied = combinedStress;
        return combinedStress;
    }
    //
    private float getCombinedStress() {
        if (level == null) return 0;
        return -power/speed;// ? it's weird to do that but...
    }

    public float pressureRatio() {
        //depend on speed ?
        return 2;
    }

    //nope -> we're gonna do that an other way : speed will fix flow and pressure is fixed
    // it's directional
    @Override
    public void lazyTick() {
    }

}
