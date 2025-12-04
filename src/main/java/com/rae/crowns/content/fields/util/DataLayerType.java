    package com.rae.crowns.content.fields.util;

import com.mojang.datafixers.util.Function3;
import com.rae.crowns.content.fields.temperature.ConductionDataLayer;
import com.rae.crowns.content.fields.temperature.ResilienceDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class DataLayerType<T extends AbstractDataLayer> {
    public static final Map<String, DataLayerType<?>> REGISTRY = new HashMap<>();

    public static final DataLayerType<TemperatureDataLayer> TEMPERATURE = register("temperature", TemperatureDataLayer::new,
            PhysicsSaveManager::getDefaultTemperature);
    public static final DataLayerType<TemperatureDataLayer> DEFAULT_TEMPERATURE = register("default_temperature",
            TemperatureDataLayer::new,PhysicsSaveManager::getDefaultTemperature);
    public static final DataLayerType<ResilienceDataLayer> RESILIENCE = register("resilence", ResilienceDataLayer::new,
            (level, pos, blockState) -> PhysicsSaveManager.getDefaultResilience(blockState));
    public static final DataLayerType<ConductionDataLayer> CONDUCTION = register("conduction", ConductionDataLayer::new,
            (level, pos, blockState) -> PhysicsSaveManager.getDefaultConduction(blockState));

    public final String id;
    private final Supplier<T> factory;
    private final Function3<Level, BlockPos, BlockState, Float> initializer;

    private DataLayerType(String id, Supplier<T> factory, Function3<Level, BlockPos, BlockState, Float> initializer) {
        this.id = id;
        this.factory = factory;
        this.initializer = initializer;
    }
    public Function3<Level, BlockPos, BlockState, Float> getInitializer() {
        return initializer;
    }
    public T createLayer() {
        return factory.get();
    }
    public static <T extends AbstractDataLayer> DataLayerType<T> register(String id, Supplier<T> factory, Function3<Level, BlockPos, BlockState, Float> initializer) {
        DataLayerType<T> type = new DataLayerType<>(id, factory, initializer);
        REGISTRY.put(id, type);
        return type;
    }


}