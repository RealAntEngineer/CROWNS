package com.rae.crowns.mixin;

import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerMixin extends SmartBlockEntity implements IHaveTemperature {

    public BlazeBurnerMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow(remap = false)
    protected abstract BlazeBurnerBlock.HeatLevel getHeatLevel();

    @Shadow(remap = false)
    public abstract BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock();

    @Override
    public void initialize() {
        super.initialize();
        /*if (level instanceof ServerLevel serverLevel) {
            TemperatureWorldData data = TemperatureManager.get(serverLevel);
            data.putDynamic(getBlockPos(), this);
        }*/
    }

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
        return switch (getHeatLevelFromBlock()) {
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
