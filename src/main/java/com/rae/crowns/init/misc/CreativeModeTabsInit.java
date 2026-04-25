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

                        double u235Mol = getMol(grade, 3_000_000, 235);

                        double u238Mol = getMol(1.0 - grade, 3_000_000, 238);

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U235.getId()), (float) u235Mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U238.getId()), (float) u238Mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    }

                    {
                        // MOX
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();

                        double u238mol = getMol(0.8, 3_000_000, 238);
                        double u235mol = getMol(0.05, 3_000_000, 235);
                        double pu239mol = getMol(0.15, 3_000_000, 239);

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U238.getId()), (float) u238mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U235.getId()), (float) u235mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.Pu239.getId()), (float) pu239mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    }


                    {
                        // Neutron source
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();

                        double am241mol = getMol(0.4, 3_000_000, 241);
                        double be9Mol = getMol(0.1, 3_000_000, 9);

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat(String.valueOf(NucleusInit.Am241Be.getId()), (float) am241mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.Be9.getId()), (float) be9Mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    }

                    return itemStacks;
                },
                BlockInit.FUEL_ASSEMBLY, item -> {
                    Collection<ItemStack> itemStacks = new ArrayList<>();
                    for (double grade : uraniumGrades) {
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();

                        double u235Mol = getMol(grade * ASSEMBLY_FACTOR, 3_000_000, 235);

                        double u238Mol = getMol((1.0 - grade) * ASSEMBLY_FACTOR, 3_000_000, 238);

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U235.getId()), (float) u235Mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U238.getId()), (float) u238Mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    }

                    {
                        // MOX
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();

                        double u238mol = NucleusInit.U238.massToMole((float) (0.8 * ASSEMBLY_FACTOR * 3_000_000));
                        double u235mol = getMol(0.05 * ASSEMBLY_FACTOR, 3_000_000, 235);
                        double pu239mol = getMol(0.15 * ASSEMBLY_FACTOR, 3_000_000, 239);

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U238.getId()), (float) u238mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.U235.getId()), (float) u235mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.Pu239.getId()), (float) pu239mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    }


                    {
                        // Neutron source
                        ItemStack itemStack = item.getDefaultInstance();
                        CompoundTag tag = itemStack.getOrCreateTag();

                        double am241mol = getMol(0.4, 3_000_000, 241);
                        double be9Mol = getMol(0.1, 3_000_000, 9);

                        CompoundTag compositionNBT = new CompoundTag();
                        compositionNBT.putFloat(String.valueOf(NucleusInit.Am241Be.getId()), (float) am241mol);
                        compositionNBT.putFloat(String.valueOf(NucleusInit.Be9.getId()), (float) be9Mol);

                        tag.put("composition", compositionNBT);
                        itemStack.setTag(tag);
                        itemStacks.add(itemStack);
                    } // Scoping hacks my beloved

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

    //TODO replace by Nucleus::massToMol
    private static double getMol(double percent, double mass, double amu) {
        double grams = mass * percent;
        return grams / amu;
    }


    public static void register(IEventBus modEventBus) {
        TAB_REGISTER.register(modEventBus);
    }
}
