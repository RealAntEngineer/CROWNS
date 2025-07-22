package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import com.simibubi.create.AllCreativeModeTabs;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.function.Function;

import static net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack.FLUID_NBT_KEY;

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
                                output.accept(BlockInit.STEAM_COLLECTOR);
                                output.accept(BlockInit.TURBINE_STAGE);
                                output.accept(BlockInit.COMPRESSOR);
                                output.acceptAll(makeFuelAssembly().apply(BlockInit.FUEL_ASSEMBLY.asItem()));
                                output.accept(BlockInit.DEEP_URANIUM_ORE);
                                output.accept(BlockInit.URANIUM_ORE);
                                output.accept(ItemInit.URANIUM_INGOT);
                                output.accept(ItemInit.RAW_URANIUM);

                            })
                            .build());
    private static Function<Item, Collection<ItemStack>> makeFuelAssembly() {
        Map<Item, Function<Item, Collection<ItemStack>>> factories = new Reference2ReferenceOpenHashMap<>();
        List<Float> uraniumGrades = List.of(7e-4f, 5e-3f, 0.2f, 0.9f);
        Map<ItemProviderEntry<?>, Function<Item, Collection<ItemStack>>> simpleFactories = Map.of(
                BlockInit.FUEL_ASSEMBLY, item -> {
                    Collection<ItemStack> itemStacks = new ArrayList<>();
                    for (Float grade : uraniumGrades) {
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();
                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat("crowns:u235", grade*0.2f);
                        compositionNBT.putFloat("crowns:u238", (1-grade)*0.2f);
                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);

                    }
                    return itemStacks;
                }
        );

        simpleFactories.forEach((entry, factory) -> {
            factories.put(entry.asItem(), factory);
        });

        return item -> {
            Function<Item, Collection<ItemStack>> factory = factories.get(item);
            if (factory != null) {
                return factory.apply(item);
            }
            return Collections.singleton(new ItemStack(item));
        };
    }

    public static void register(IEventBus modEventBus) {
        TAB_REGISTER.register(modEventBus);
    }
}
