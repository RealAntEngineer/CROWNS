    package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.advection.BlockedDataLayer;
import com.rae.crowns.content.fields.advection.VelocityDataLayer;
import com.rae.crowns.content.fields.temperature.ConductionDataLayer;
import com.rae.crowns.content.fields.temperature.ResilienceDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class DataLayerType<T extends AbstractDataLayer> {
    public static final Map<String, DataLayerType<?>> REGISTRY = new HashMap<>();

    public static final DataLayerType<TemperatureDataLayer> TEMPERATURE = register("temperature", TemperatureDataLayer::new);
    public static final DataLayerType<TemperatureDataLayer> DEFAULT_TEMPERATURE = register("default_temperature", TemperatureDataLayer::new);
    public static final DataLayerType<ResilienceDataLayer> RESILIENCE = register("resilence", ResilienceDataLayer::new);
    public static final DataLayerType<ConductionDataLayer> CONDUCTION = register("conduction", ConductionDataLayer::new);
    public static final DataLayerType<VelocityDataLayer> VX = register("vx", VelocityDataLayer::new);
    public static final DataLayerType<BlockedDataLayer> BLOCKED_X = register("blocked_x", BlockedDataLayer::new);
    public static final DataLayerType<VelocityDataLayer> VY = register("vy", VelocityDataLayer::new);
    public static final DataLayerType<BlockedDataLayer> BLOCKED_Y = register("blocked_y", BlockedDataLayer::new);
    public static final DataLayerType<VelocityDataLayer> VZ = register("vz", VelocityDataLayer::new);
    public static final DataLayerType<BlockedDataLayer> BLOCKED_Z = register("blocked_z", BlockedDataLayer::new);


    public final String id;
    private final Supplier<T> factory;

    private DataLayerType(String id, Supplier<T> factory) {
        this.id = id;
        this.factory = factory;
    }

    public T createLayer() {
        return factory.get();
    }
    public static <T extends AbstractDataLayer> DataLayerType<T> register(String id, Supplier<T> factory) {
        DataLayerType<T> type = new DataLayerType<>(id, factory);
        REGISTRY.put(id, type);
        return type;
    }
}