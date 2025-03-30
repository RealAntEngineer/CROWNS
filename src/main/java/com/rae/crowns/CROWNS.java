package com.rae.crowns;

import com.mojang.logging.LogUtils;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.init.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;

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
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    public CROWNS(IEventBus modEventBus, ModContainer modContainer){
        IEventBus forgeEventBus = NeoForge.EVENT_BUS;
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        REGISTRATE.registerEventListeners(modEventBus);
        TagsInit.init();

        BlockInit.register();
        ItemInit.register();
        BlockEntityInit.register();
        EntityInit.register();

        CreativeModeTabsInit.register(modEventBus);
        ParticleTypeInit.register(modEventBus);
        PartialModelInit.init();
        EntityDataSerializersInit.register(modEventBus);

        CROWNSConfigs.registerConfigs(modLoadingContext,modContainer);
        CROWNSContraptionType.prepare();
        //CreativeModeTabsInit.init();

        forgeEventBus.addListener(CROWNS::onAddReloadListeners);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        //event.addListener(VaporTableDataProcessor.DATA_TABLE_HOLDER);
    }

    public static ResourceLocation resource(String name) {
            return ResourceLocation.fromNamespaceAndPath(MODID, name);
        }

    }
