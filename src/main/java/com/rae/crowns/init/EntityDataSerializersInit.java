package com.rae.crowns.init;

import com.rae.crowns.CROWNS;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;


public class EntityDataSerializersInit {
    private static final DeferredRegister<EntityDataSerializer<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, CROWNS.MODID);
    public static final AABBSerializer BB_SERIALIZER = new AABBSerializer();
    public static final StateMapSerializer SM_SERIALIZER = new StateMapSerializer();


    public static final DeferredHolder<EntityDataSerializer<?>, AABBSerializer> SHAPE_DATA_ENTRY = REGISTER.register("aabb", () -> BB_SERIALIZER);
    public static final DeferredHolder<EntityDataSerializer<?>, StateMapSerializer> STATE_MAP_DATA_ENTRY = REGISTER.register("state_map", () -> SM_SERIALIZER);

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
