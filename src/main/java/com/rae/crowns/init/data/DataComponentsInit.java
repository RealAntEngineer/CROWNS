package com.rae.crowns.init.data;

import com.rae.crowns.CROWNS;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.UnaryOperator;

public class DataComponentsInit {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CROWNS.MODID);

    public static final DataComponentType<SpecificRealGasState> REAL_GAS_STATE = register(
            "real_gas_state",
            builder ->
                    builder.persistent(SpecificRealGasState.CODEC)
                            .networkSynchronized(SpecificRealGasState.STREAM_CODEC)
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    @ApiStatus.Internal
    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
