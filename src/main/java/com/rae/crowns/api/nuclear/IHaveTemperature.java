package com.rae.crowns.api.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface IHaveTemperature {

    float getThermalCapacity();
    float getThermalConductivity();
    float getTemperature();
    void addTemperature(float dT);
    default void conductTemperature(BlockPos pos, Level level){
        conductTemperature(pos, level, 1);
    }
    default void conductTemperature(BlockPos pos, Level level, float dt){
        for (Direction direction: Direction.stream().toList()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            BlockEntity be = level.getBlockEntity(pos.relative(direction));

            if (be instanceof IHaveTemperature iHaveTemperature) {
                float transmittedPower = (iHaveTemperature.getTemperature() - this.getTemperature()) *
                        (getThermalConductivity() + iHaveTemperature.getThermalConductivity()) / 2 *dt;
                this.addTemperature(
                        transmittedPower
                                / this.getThermalCapacity());
                //iHaveTemperature.addTemperature(-transmittedPower / iHaveTemperature.getThermalCapacity());
            } else {
                FluidState fluidState = level.getFluidState(pos.relative(direction));
                int T = 300;
                if (state.is(Blocks.ICE)){
                    T = 273;
                }
                if (state.is(Blocks.PACKED_ICE)){
                    T = 220;
                }
                if (!fluidState.isEmpty() && fluidState.is(FluidTags.LAVA)){
                    T = 900;
                }
                addTemperature((T - getTemperature()) * this.getThermalConductivity() / getThermalCapacity());
            }
        }
    }
}