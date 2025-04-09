package com.rae.crowns.init.data;

import com.google.common.base.Predicates;
import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class AttachementTypeInit {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CROWNS.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<TemperatureDataLayer>>> CHUNK_TEMPERATURE = ATTACHMENTS.register(
            "temperature",
            () -> AttachmentType.builder(() -> (List<TemperatureDataLayer>) new ArrayList<TemperatureDataLayer>())
                    .serialize(TemperatureManager.CODEC, Predicates.alwaysTrue())
                    .copyHandler(TemperatureManager::copyTemperature)
                    .build()
    );

    public static void register(IEventBus modEventBus){
        ATTACHMENTS.register(modEventBus);
    }
}
