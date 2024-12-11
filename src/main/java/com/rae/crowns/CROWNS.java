package com.rae.crowns;

import com.mojang.logging.LogUtils;
import com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.init.*;
import com.rae.crowns.init.CROWNSContraptionType;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CROWNS.MODID)//CreatingRotationOperatedWithNuclearScience
public class CROWNS {
    public static final String MODID = "crowns";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    public CROWNS(){
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        REGISTRATE.registerEventListeners(modEventBus);
        TagsInit.init();

        BlockInit.register();
        BlockEntityInit.register();
        EntityInit.register();

        ParticleTypeInit.register(modEventBus);
        PartialModelInit.init();
        EntityDataSerializersInit.register(modEventBus);

        CROWNSConfigs.registerConfigs(modLoadingContext);
        CROWNSContraptionType.prepare();
        WaterAsRealGazTransformationHelper.init();

        //CreativeModeTabsInit.init();

        forgeEventBus.addListener(CROWNS::onAddReloadListeners);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->  CROWNSClient.clientRegister(modEventBus));

    }

    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        //event.addListener(VaporTableDataProcessor.DATA_TABLE_HOLDER);
    }

    public static ResourceLocation resource(String name) {
        return new ResourceLocation(MODID,name);
    }
}
