package com.rae.crowns.content.nuclear.display;

import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactorMonitorBE extends SmartBlockEntity {
    //block pos is the position at the top of the stack
    // list is the list of Assembly block entities ordered from the top down
    Map<BlockPos, List<AssemblyBlockEntity>> assemblyBlockEntities = new HashMap<>();


    public ReactorMonitorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }
}
