package com.rae.crowns.content.thermodynamics.conduction;

import com.rae.crowns.CROWNS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface IHaveTemperature {

    float getThermalCapacity();
    float getThermalConductivity();
    float getTemperature();
    void addTemperature(float dT);
    /*
    default void conductTemperature(BlockPos pos, Level level){
        conductTemperature(pos, level, 1);
    }
    default void conductTemperature(BlockPos pos, Level level, float dt){
        for (Direction direction: Direction.stream().toList()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            BlockEntity be = level.getBlockEntity(pos.relative(direction));

            if (be instanceof IHaveTemperature iHaveTemperature) {
                float transmittedPower;
                if (iHaveTemperature.getThermalConductivity() == 0 && this.getThermalConductivity() == 0){
                    transmittedPower = 0.0f;
                }
                else {
                    transmittedPower = (iHaveTemperature.getTemperature() - this.getTemperature()) *
                            (getThermalConductivity() * iHaveTemperature.getThermalConductivity()) /
                            (getThermalConductivity() + iHaveTemperature.getThermalConductivity()) * dt;
                }
                this.addTemperature(
                        transmittedPower
                                / this.getThermalCapacity());
                //iHaveTemperature.addTemperature(-transmittedPower / iHaveTemperature.getThermalCapacity());
            } else {
                FluidState fluidState = level.getFluidState(pos.relative(direction));
                float T;
                if (fluidState.isEmpty()) {
                    T = CROWNS.BLOCK_TEMPERATURES.getValue(state.getBlock(), 300f);
                }
                else {
                    T = CROWNS.FLUID_TEMPERATURES.getValue(fluidState.getType(), 300f);

                }
                addTemperature((T - getTemperature()) * this.getThermalConductivity() / this.getThermalCapacity());
            }
        }
    }*/
}