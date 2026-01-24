package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.display.FuelStackDisplaySource;
import com.rae.crowns.content.nuclear.display.RadiationSourceDisplaySource;
import com.rae.crowns.content.nuclear.display.TemperatureDisplaySource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static com.rae.crowns.CROWNS.REGISTRATE;

public class DisplaySourceInit {
    public static final RegistryEntry<DisplaySource> ACTIVITY = simple("radiation_source", RadiationSourceDisplaySource::new);
    public static final RegistryEntry<DisplaySource> TEMPERATURE = simple("temperature", TemperatureDisplaySource::new);
    public static final RegistryEntry<DisplaySource> FULL_STACK = simple("full_stack", FuelStackDisplaySource::new);

    private static <T extends DisplaySource> @NotNull RegistryEntry<T> simple(String name, Supplier<T> supplier) {
        return REGISTRATE.displaySource(name, supplier).register();
    }

    public static void register() {
    }
}
