package com.rae.crowns.mixin;

import com.rae.crowns.content.thermodynamics.conduction.IHaveTemperature;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerMixin implements IHaveTemperature {

    @Shadow(remap = false) protected abstract BlazeBurnerBlock.HeatLevel getHeatLevel();

    @Shadow(remap = false) public abstract BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock();

    @Override
    public float getThermalCapacity() {
        return 1000;
    }

    @Override
    public float getThermalConductivity() {
        return 100000;
    }

    @Override
    public float getTemperature() {
        return switch (getHeatLevelFromBlock()){
            case NONE -> 300f;
            case SMOULDERING -> 500F;
            case FADING -> 900F;
            case KINDLED -> 1800F;
            case SEETHING -> 3000F;
        };
    }

    @Override
    public void addTemperature(float dT) {
    }
}
