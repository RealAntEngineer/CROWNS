package com.rae.crowns.init;

import com.rae.crowns.CROWNS;
import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeModeTabsInit {
    private static final DeferredRegister<CreativeModeTab> TAB_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CROWNS.MODID);


    public static final RegistryObject<CreativeModeTab> NUCLEAR_TAB =
            TAB_REGISTER.register("nuclear",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.crowns.nuclear"))
                            .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey())
                            .icon(BlockInit.FUEL_ASSEMBLY::asStack)
                            .displayItems(($1,output)-> {
                                output.accept(BlockInit.HEAT_EXCHANGER);
                                output.accept(BlockInit.STEAM_INPUT);
                                output.accept(BlockInit.TURBINE_STAGE);
                                output.accept(BlockInit.COMPRESSOR);
                                output.accept(BlockInit.FUEL_ASSEMBLY);
                                output.accept(BlockInit.DEEP_URANIUM_ORE);
                                output.accept(BlockInit.URANIUM_ORE);
                                output.accept(ItemInit.URANIUM_INGOT);
                                output.accept(ItemInit.RAW_URANIUM);

                            })
                            .build());


    public static void register(IEventBus modEventBus) {
        TAB_REGISTER.register(modEventBus);
    }
}
