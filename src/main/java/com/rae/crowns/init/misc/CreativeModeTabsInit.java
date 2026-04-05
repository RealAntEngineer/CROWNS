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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class CreativeModeTabsInit {
    private static final DeferredRegister<CreativeModeTab> TAB_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CROWNS.MODID);


    public static final RegistryObject<CreativeModeTab> NUCLEAR_TAB =
            TAB_REGISTER.register("nuclear",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.crowns.nuclear"))
                            .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey())
                            .icon(BlockInit.FUEL_ASSEMBLY::asStack)
                            .displayItems(($1, output) -> {
                                output.accept(BlockInit.HEAT_EXCHANGER);
                                output.accept(BlockInit.STEAM_INPUT);
                                output.accept(BlockInit.STEAM_COLLECTOR);
                                output.accept(BlockInit.TURBINE_STAGE);
                                output.accept(BlockInit.COMPRESSOR);
                                output.acceptAll(makeFuelAssembly().apply(BlockInit.FUEL_ASSEMBLY.asItem()));
                                output.accept(FluidInit.CORIUM.get().getBucket());
                                output.accept(BlockInit.SOLID_CORIUM);
                                output.accept(BlockInit.DEEP_URANIUM_ORE);
                                output.accept(BlockInit.URANIUM_ORE);
                                output.accept(BlockInit.DEPLETED_URANIUM_BLOCK);
                                output.accept(ItemInit.RAW_URANIUM);
                                output.accept(ItemInit.URANIUM_INGOT);
                                output.accept(ItemInit.NATURAL_URANIUM_NUGGET);
                                output.accept(ItemInit.DEPLETED_URANIUM_INGOT);
                                output.accept(ItemInit.DEPLETED_URANIUM_NUGGET);
                                output.accept(ItemInit.ENRICHED_URANIUM_INGOT);
                                output.accept(ItemInit.ENRICHED_URANIUM_NUGGET);
                                output.accept(FluidInit.URANIUM_HEXAFLUORIDE.get().getBucket());
                                output.acceptAll(makeFuelAssembly().apply(ItemInit.FUEL_ROD.asItem()));
                                output.accept(BlockInit.REACTOR_CASING);
                                output.accept(BlockInit.REACTOR_VESSEL);
                                output.accept(ItemInit.DOSIMETER);
                            })
                            .build());

    private static @NotNull Function<Item, Collection<ItemStack>> makeFuelAssembly() {
        Map<Item, Function<Item, Collection<ItemStack>>> factories = new Reference2ReferenceOpenHashMap<>();

        // exact doubles
        List<Double> uraniumGrades = List.of(
                1.0 / 128.0, // 0.078125
                3.0 / 16.0,  // 0.1875
                7.0 / 8.0   // 0.875
        );

        final double ASSEMBLY_FACTOR = 1.0 / 4.0; // 0.25

        Map<ItemProviderEntry<?>, Function<Item, Collection<ItemStack>>> simpleFactories = Map.of(
                ItemInit.FUEL_ROD, item -> {
                    Collection<ItemStack> itemStacks = new ArrayList<>();
                    for (double grade : uraniumGrades) {
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();

                        double u235Percent = grade * ASSEMBLY_FACTOR;
                        double u235g = 3_000_000 * u235Percent;
                        double u235Mol = u235g / 235;

                        double u238Percent = (1.0 - grade) * ASSEMBLY_FACTOR;
                        double u238g = 3_000_000 * u238Percent;
                        double u238Mol = u238g / 238;

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat("crowns.nucleus.235", (float) u235Mol);
                        compositionNBT.putFloat("crowns.nucleus.238", (float) u238Mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    }

                    // Neutron source
                    ItemStack itemStack = item.getDefaultInstance();
                    CompoundTag tag = itemStack.getOrCreateTag();

                    double am241Percent = 0.4;
                    double am241g = 3_000_000 * am241Percent;
                    double am241mol = am241g / 241;

                    double be9Percent = 0.1;
                    double be9g = 3_000_000 * be9Percent;
                    double be9Mol = be9g / 9;

                    CompoundTag compositionNBT = new CompoundTag();
                    compositionNBT.putFloat("crowns.nucleus.241", (float) am241mol);
                    compositionNBT.putFloat("crowns.nucleus.9", (float) be9Mol);

                    tag.put("composition", compositionNBT);
                    itemStack.setTag(tag);
                    itemStacks.add(itemStack);

                    return itemStacks;
                },
                BlockInit.FUEL_ASSEMBLY, item -> {
                    Collection<ItemStack> itemStacks = new ArrayList<>();
                    for (double grade : uraniumGrades) {
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();

                        double u235Percent = grade * ASSEMBLY_FACTOR;
                        double u235g = 3_000_000 * u235Percent;
                        double u235Mol = u235g / 235;

                        double u238Percent = (1.0 - grade) * ASSEMBLY_FACTOR;
                        double u238g = 3_000_000 * u238Percent;
                        double u238Mol = u238g / 238;

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat("crowns.nucleus.235", (float) u235Mol);
                        compositionNBT.putFloat("crowns.nucleus.238", (float) u238Mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    }

                    // Neutron source
                    ItemStack itemStack = item.getDefaultInstance();
                    CompoundTag tag = itemStack.getOrCreateTag();

                    double am241Percent = 0.4 * ASSEMBLY_FACTOR;
                    double am241g = 3_000_000 * am241Percent;
                    double am241mol = am241g / 241;

                    double be9Percent = 0.1 * ASSEMBLY_FACTOR;
                    double be9g = 3_000_000 * be9Percent;
                    double be9Mol = be9g / 9;

                    CompoundTag compositionNBT = new CompoundTag();
                    compositionNBT.putFloat("crowns.nucleus.241", (float) am241mol);
                    compositionNBT.putFloat("crowns.nucleus.9", (float) be9Mol);

                    tag.put("composition", compositionNBT);
                    itemStack.setTag(tag);
                    itemStacks.add(itemStack);

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
