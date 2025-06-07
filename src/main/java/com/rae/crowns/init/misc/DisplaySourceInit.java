package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.RadiationSourceDisplaySource;
import com.rae.crowns.content.nuclear.TemperatureDisplaySource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;

import java.util.function.Supplier;

import static com.rae.crowns.CROWNS.REGISTRATE;

public class DisplaySourceInit {
    public static final RegistryEntry<DisplaySource, RadiationSourceDisplaySource> ACTIVITY = simple("radiation_source", RadiationSourceDisplaySource::new);
    public static final RegistryEntry<DisplaySource, TemperatureDisplaySource> TEMPERATURE = simple("temperature", TemperatureDisplaySource::new);

    private static <T extends DisplaySource> RegistryEntry<DisplaySource, T> simple(String name, Supplier<T> supplier) {
        return REGISTRATE.displaySource(name, supplier).register();
    }

    public static void register() {
    }
}
