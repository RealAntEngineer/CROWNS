package com.rae.crowns;

import com.mojang.logging.LogUtils;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.init.client.PartialModelInit;
import com.rae.crowns.init.client.ParticleTypeInit;
import com.rae.crowns.init.client.SoundInit;
import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.crowns.init.data.EntityDataSerializersInit;
import com.rae.crowns.init.data.PacketInit;
import com.rae.crowns.init.misc.*;
import com.rae.formicapi.data.managers.FloatMapDataLoader;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@SuppressWarnings("ALL")
@Mod(CROWNS.MODID)//CreatingRotationOperatedWithNuclearScience
public class CROWNS {
    public static final String MODID = "crowns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE =
            CreateRegistrate.create(MODID)
                    .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    public static final FloatMapDataLoader<Block> BLOCK_TEMPERATURES = new FloatMapDataLoader<>(MODID, "blocks/temperatures", Registries.BLOCK);
    public static final FloatMapDataLoader<Block> BLOCK_CONDUCTION = new FloatMapDataLoader<>(MODID, "blocks/conduction", Registries.BLOCK);
    public static final FloatMapDataLoader<Block> BLOCK_RESILIENCE = new FloatMapDataLoader<>(MODID, "blocks/resilience", Registries.BLOCK);
    public static final FloatMapDataLoader<Fluid> FLUID_TEMPERATURES = new FloatMapDataLoader<>(MODID, "fluids/temperatures", Registries.FLUID);
    public static final FloatMapDataLoader<Fluid> FLUID_CONDUCTION = new FloatMapDataLoader<>(MODID, "fluids/conduction", Registries.FLUID);
    public static final FloatMapDataLoader<Fluid> FLUID_RESILIENCE = new FloatMapDataLoader<>(MODID, "fluids/resilience", Registries.FLUID);


    public static final FloatMapDataLoader<Biome> BIOME_TEMPERATURES = new FloatMapDataLoader<>(MODID, "biomes/temperatures", Registries.BIOME);


    public CROWNS(IEventBus modEventBus, ModContainer modContainer) {
        IEventBus forgeEventBus = NeoForge.EVENT_BUS;
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        REGISTRATE.registerEventListeners(modEventBus);
        TagsInit.init();

        BlockInit.register();
        ItemInit.register();
        FluidInit.register();
        BlockEntityInit.register();
        EntityInit.register();
        SoundInit.register(modEventBus);

        PacketInit.register();
        DisplaySourceInit.register();
        CreativeModeTabsInit.register(modEventBus);
        ParticleTypeInit.register(modEventBus);
        DataComponentsInit.register(modEventBus);
        PartialModelInit.init();
        EntityDataSerializersInit.register(modEventBus);

        CROWNSConfigs.registerConfigs(modLoadingContext, modContainer);
        CROWNSContraptionType.prepare();
        MovementCheckInit.register();
        //CreativeModeTabsInit.init();

        forgeEventBus.addListener(CROWNS::onAddReloadListeners);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CROWNS.BLOCK_TEMPERATURES);
        event.addListener(CROWNS.BLOCK_RESILIENCE);
        event.addListener(CROWNS.BLOCK_CONDUCTION);

        event.addListener(CROWNS.FLUID_TEMPERATURES);
        event.addListener(CROWNS.FLUID_RESILIENCE);
        event.addListener(CROWNS.FLUID_CONDUCTION);

        event.addListener(CROWNS.BIOME_TEMPERATURES);

    }

    public static ResourceLocation resource(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }

}
