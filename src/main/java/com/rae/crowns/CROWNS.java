package com.rae.crowns;

import com.mojang.logging.LogUtils;
import com.rae.colony_api.data.managers.FloatMapDataLoader;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.init.client.PartialModelInit;
import com.rae.crowns.init.client.ParticleTypeInit;
import com.rae.crowns.init.data.EntityDataSerializersInit;
import com.rae.crowns.init.misc.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@SuppressWarnings("ALL")
@Mod(CROWNS.MODID)//CreatingRotationOperatedWithNuclearScience
public class CROWNS {
    public static final String MODID = "crowns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);
    public static final FloatMapDataLoader<Block> BLOCK_TEMPERATURES = new FloatMapDataLoader<>(MODID,"block_temperatures", ForgeRegistries.BLOCKS.getRegistryKey());
    public static final FloatMapDataLoader<Fluid> FLUID_TEMPERATURES = new FloatMapDataLoader<>(MODID,"fluid_temperatures", ForgeRegistries.FLUIDS.getRegistryKey());

    public CROWNS(){
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        REGISTRATE.registerEventListeners(modEventBus);
        TagsInit.init();

        BlockInit.register();
        ItemInit.register();
        BlockEntityInit.register();
        EntityInit.register();

        DisplaySourceInit.register();
        CreativeModeTabsInit.register(modEventBus);
        ParticleTypeInit.register(modEventBus);
        PartialModelInit.init();
        EntityDataSerializersInit.register(modEventBus);

        CROWNSConfigs.registerConfigs(modLoadingContext);
        CROWNSContraptionType.prepare();
        MovementCheckInit.register();
        //CreativeModeTabsInit.init();

        forgeEventBus.addListener(CROWNS::onAddReloadListeners);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->  CROWNSClient.clientRegister(modEventBus));

    }

    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(CROWNS.BLOCK_TEMPERATURES);
        event.addListener(CROWNS.FLUID_TEMPERATURES);
    }

    public static ResourceLocation resource(String name) {
        return new ResourceLocation(MODID,name);
    }
}
