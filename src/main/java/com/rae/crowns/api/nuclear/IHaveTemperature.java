package com.rae.crowns.api.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
                addTemperature((300 - getTemperature()) * this.getThermalConductivity() / getThermalCapacity());
            }
        }
    }
}