package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.AssemblyBlock;
import com.simibubi.create.api.contraption.BlockMovementChecks;

public class MovementCheckInit {
    public static void register() {

        BlockMovementChecks.registerAttachedCheck(
                (state, world, pos, direction) -> {
                    if (state.getBlock() instanceof AssemblyBlock) {
                        if (direction.getAxis() == state.getValue(AssemblyBlock.AXIS) &&
                                world.getBlockState(pos.relative(direction)).getBlock() instanceof AssemblyBlock) {
                            return BlockMovementChecks.CheckResult.SUCCESS;
                        }
                    }
                    return BlockMovementChecks.CheckResult.PASS;
                }
        );
    }
}