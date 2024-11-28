package com.rae.crowns.init;

import com.rae.crowns.CROWNS;
import com.simibubi.create.Create;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityDataSerializersInit {
    private static final DeferredRegister<EntityDataSerializer<?>> REGISTER = DeferredRegister.create(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, CROWNS.MODID);
    public static final AABBSerializer BB_SERIALIZER = new AABBSerializer();

    public static final RegistryObject<AABBSerializer> SHAPE_DATA_ENTRY = REGISTER.register("aabb", () -> BB_SERIALIZER);

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
