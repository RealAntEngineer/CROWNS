package com.rae.crowns.init.client;

import com.rae.crowns.CROWNS;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.rae.crowns.CROWNS.MODID;

public class SoundInit {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE_SOUND = registerSound("turbine_sound");

    public static DeferredHolder<SoundEvent, SoundEvent> registerSound(String id) {
        return SOUNDS.register(id,
                () -> SoundEvent.createVariableRangeEvent(CROWNS.resource(id)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

}
