package com.rae.crowns.content.fields.util;

import com.mojang.datafixers.util.Function3;
import com.rae.crowns.content.fields.temperature.ConductionDataLayer;
import com.rae.crowns.content.fields.temperature.ResilienceDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.NonnullDefault;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@NonnullDefault
public enum DataLayerType {
    TEMPERATURE("temperature", TemperatureDataLayer::new,
            PhysicsSaveManager::getDefaultTemperature),
    DEFAULT_TEMPERATURE("default_temperature",
            TemperatureDataLayer::new, PhysicsSaveManager::getDefaultTemperature),
    RESILIENCE("resilence", ResilienceDataLayer::new,
            (level, pos, blockState) -> PhysicsSaveManager.getDefaultResilience(blockState)),
    CONDUCTION("conduction", ConductionDataLayer::new,
            (level, pos, blockState) -> PhysicsSaveManager.getDefaultConduction(blockState));

    public static final Map<String, DataLayerType> REGISTRY = new HashMap<>();
    static  {
        for (DataLayerType type : values()){
            REGISTRY.put(type.id, type);
        }
    }
    public final  String                                        id;
    private final Supplier<AbstractDataLayer>                                   factory;
    private final Function3<Level, BlockPos, BlockState, Float> initializer;

    DataLayerType(String id, Supplier<AbstractDataLayer> factory, Function3<Level, BlockPos, BlockState, Float> initializer) {
        this.id = id;
        this.factory = factory;
        this.initializer = initializer;
    }

    public Function3<Level, BlockPos, BlockState, Float> getInitializer() {
        return initializer;
    }

    public AbstractDataLayer createLayer() {
        return factory.get();
    }

    @Override
    public String toString() {
        return "DataLayerType{id='%s'}".formatted(id);
    }
}