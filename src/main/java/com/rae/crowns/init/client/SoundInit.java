package com.rae.crowns.init.client;

import com.rae.crowns.CROWNS;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.rae.crowns.CROWNS.MODID;

public class SoundInit {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<SoundEvent> TURBINE_SOUND = registerSound("turbine_sound");

    public static RegistryObject<SoundEvent> registerSound(String id) {
        return SOUNDS.register(id,
                () -> SoundEvent.createVariableRangeEvent(CROWNS.resource(id)));
    }
    public static void register() {
        SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

}
