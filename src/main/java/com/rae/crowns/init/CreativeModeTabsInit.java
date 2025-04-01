package com.rae.crowns.init;

import com.rae.crowns.CROWNS;
import com.simibubi.create.*;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlock;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.TagDependentIngredientItem;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.*;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class CreativeModeTabsInit {
    private static final DeferredRegister<CreativeModeTab> TAB_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CROWNS.MODID);


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NUCLEAR_TAB =
            TAB_REGISTER.register("nuclear",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.crowns.nuclear"))
                            .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey())
                            .icon(BlockInit.FUEL_ASSEMBLY::asStack)
                            .displayItems(new RegistrateDisplayItemsGenerator(true, CreativeModeTabsInit.NUCLEAR_TAB))

                            .build());


    public static void register(IEventBus modEventBus) {
        TAB_REGISTER.register(modEventBus);
    }

    private static class RegistrateDisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {
        private static final Predicate<Item> IS_ITEM_3D_PREDICATE;

        static {
            MutableObject<Predicate<Item>> isItem3d = new MutableObject<>(item -> false);
            if (CatnipServices.PLATFORM.getEnv().isClient())
                isItem3d.setValue(makeClient3dItemPredicate());
            IS_ITEM_3D_PREDICATE = isItem3d.getValue();
        }

        @OnlyIn(Dist.CLIENT)
        private static Predicate<Item> makeClient3dItemPredicate() {
            return item -> {
                ItemRenderer itemRenderer = Minecraft.getInstance()
                        .getItemRenderer();
                BakedModel model = itemRenderer.getModel(new ItemStack(item), null, null, 0);
                return model.isGui3d();
            };
        }

        private final boolean addItems;
        private final DeferredHolder<CreativeModeTab, CreativeModeTab> tabFilter;

        public RegistrateDisplayItemsGenerator(boolean addItems, DeferredHolder<CreativeModeTab, CreativeModeTab> tabFilter) {
            this.addItems = addItems;
            this.tabFilter = tabFilter;
        }

        private static Predicate<Item> makeExclusionPredicate() {
            Set<Item> exclusions = new ReferenceOpenHashSet<>();

            List<ItemProviderEntry<?, ?>> simpleExclusions = List.of(
                    AllItems.INCOMPLETE_PRECISION_MECHANISM,
                    AllItems.INCOMPLETE_REINFORCED_SHEET,
                    AllItems.INCOMPLETE_TRACK,
                    AllItems.CHROMATIC_COMPOUND,
                    AllItems.SHADOW_STEEL,
                    AllItems.REFINED_RADIANCE,
                    AllItems.COPPER_BACKTANK_PLACEABLE,
                    AllItems.NETHERITE_BACKTANK_PLACEABLE,
                    AllItems.MINECART_CONTRAPTION,
                    AllItems.FURNACE_MINECART_CONTRAPTION,
                    AllItems.CHEST_MINECART_CONTRAPTION,
                    AllItems.SCHEMATIC,
                    AllItems.SHOPPING_LIST,
                    AllBlocks.ANDESITE_ENCASED_SHAFT,
                    AllBlocks.BRASS_ENCASED_SHAFT,
                    AllBlocks.ANDESITE_ENCASED_COGWHEEL,
                    AllBlocks.BRASS_ENCASED_COGWHEEL,
                    AllBlocks.ANDESITE_ENCASED_LARGE_COGWHEEL,
                    AllBlocks.BRASS_ENCASED_LARGE_COGWHEEL,
                    AllBlocks.MYSTERIOUS_CUCKOO_CLOCK,
                    AllBlocks.ELEVATOR_CONTACT,
                    AllBlocks.SHADOW_STEEL_CASING,
                    AllBlocks.REFINED_RADIANCE_CASING
            );

            List<ItemEntry<TagDependentIngredientItem>> tagDependentExclusions = List.of(
                    AllItems.CRUSHED_OSMIUM,
                    AllItems.CRUSHED_PLATINUM,
                    AllItems.CRUSHED_SILVER,
                    AllItems.CRUSHED_TIN,
                    AllItems.CRUSHED_LEAD,
                    AllItems.CRUSHED_QUICKSILVER,
                    AllItems.CRUSHED_BAUXITE,
                    AllItems.CRUSHED_URANIUM,
                    AllItems.CRUSHED_NICKEL
            );

            exclusions.addAll(PackageStyles.RARE_BOXES);

            for (ItemProviderEntry<?, ?> entry : simpleExclusions) {
                exclusions.add(entry.asItem());
            }

            for (ItemEntry<TagDependentIngredientItem> entry : tagDependentExclusions) {
                TagDependentIngredientItem item = entry.get();
                if (item.shouldHide()) {
                    exclusions.add(entry.asItem());
                }
            }

            return exclusions::contains;
        }

        private static List<RegistrateDisplayItemsGenerator.ItemOrdering> makeOrderings() {
            List<RegistrateDisplayItemsGenerator.ItemOrdering> orderings = new ReferenceArrayList<>();

            Map<ItemProviderEntry<?, ?>, ItemProviderEntry<?, ?>> simpleBeforeOrderings = Map.of(
                    AllItems.EMPTY_BLAZE_BURNER, AllBlocks.BLAZE_BURNER,
                    AllItems.SCHEDULE, AllBlocks.TRACK_STATION
            );

            Map<ItemProviderEntry<?, ?>, ItemProviderEntry<?, ?>> simpleAfterOrderings = Map.of(
                    AllItems.VERTICAL_GEARBOX, AllBlocks.GEARBOX
            );

            simpleBeforeOrderings.forEach((entry, otherEntry) -> {
                orderings.add(RegistrateDisplayItemsGenerator.ItemOrdering.before(entry.asItem(), otherEntry.asItem()));
            });

            simpleAfterOrderings.forEach((entry, otherEntry) -> {
                orderings.add(RegistrateDisplayItemsGenerator.ItemOrdering.after(entry.asItem(), otherEntry.asItem()));
            });

            PackageStyles.STANDARD_BOXES.forEach(item -> {
                orderings.add(RegistrateDisplayItemsGenerator.ItemOrdering.after(item, AllBlocks.PACKAGER.asItem()));
            });

            return orderings;
        }

        private static Function<Item, ItemStack> makeStackFunc() {
            Map<Item, Function<Item, ItemStack>> factories = new Reference2ReferenceOpenHashMap<>();

            Map<ItemProviderEntry<?, ?>, Function<Item, ItemStack>> simpleFactories = Map.of(
                    AllItems.COPPER_BACKTANK, item -> {
                        ItemStack stack = new ItemStack(item);
                        stack.set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants());
                        return stack;
                    },
                    AllItems.NETHERITE_BACKTANK, item -> {
                        ItemStack stack = new ItemStack(item);
                        stack.set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants());
                        return stack;
                    }
            );

            simpleFactories.forEach((entry, factory) -> {
                factories.put(entry.asItem(), factory);
            });

            return item -> {
                Function<Item, ItemStack> factory = factories.get(item);
                if (factory != null) {
                    return factory.apply(item);
                }
                return new ItemStack(item);
            };
        }

        private static Function<Item, CreativeModeTab.TabVisibility> makeVisibilityFunc() {
            Map<Item, CreativeModeTab.TabVisibility> visibilities = new Reference2ObjectOpenHashMap<>();

            Map<ItemProviderEntry<?, ?>, CreativeModeTab.TabVisibility> simpleVisibilities = Map.of(
                    AllItems.BLAZE_CAKE_BASE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY
            );

            simpleVisibilities.forEach((entry, factory) -> {
                visibilities.put(entry.asItem(), factory);
            });

            for (BlockEntry<ValveHandleBlock> entry : AllBlocks.DYED_VALVE_HANDLES) {
                visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
            }

            for (BlockEntry<SeatBlock> entry : AllBlocks.SEATS) {
                SeatBlock block = entry.get();
                if (block.getColor() != DyeColor.RED) {
                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                }
            }

            for (BlockEntry<TableClothBlock> entry : AllBlocks.TABLE_CLOTHS) {
                TableClothBlock block = entry.get();
                if (block.getColor() != DyeColor.RED) {
                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                }
            }

            for (BlockEntry<PostboxBlock> entry : AllBlocks.PACKAGE_POSTBOXES) {
                PostboxBlock block = entry.get();
                if (block.getColor() != DyeColor.WHITE) {
                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                }
            }

            for (BlockEntry<ToolboxBlock> entry : AllBlocks.TOOLBOXES) {
                ToolboxBlock block = entry.get();
                if (block.getColor() != DyeColor.BROWN) {
                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                }
            }

            return item -> {
                CreativeModeTab.TabVisibility visibility = visibilities.get(item);
                if (visibility != null) {
                    return visibility;
                }
                return CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            };
        }

        @Override
        public void accept(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
            Predicate<Item> exclusionPredicate = makeExclusionPredicate();
            List<RegistrateDisplayItemsGenerator.ItemOrdering> orderings = makeOrderings();
            Function<Item, ItemStack> stackFunc = makeStackFunc();
            Function<Item, CreativeModeTab.TabVisibility> visibilityFunc = makeVisibilityFunc();

            List<Item> items = new LinkedList<>();
            if (addItems) {
                items.addAll(collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE.negate())));
            }
            items.addAll(collectBlocks(exclusionPredicate));
            if (addItems) {
                items.addAll(collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE)));
            }

            applyOrderings(items, orderings);
            outputAll(output, items, stackFunc, visibilityFunc);
        }

        private List<Item> collectBlocks(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Block, Block> entry : CROWNS.REGISTRATE.getAll(Registries.BLOCK)) {

                Item item = entry.get()
                        .asItem();
                if (item == Items.AIR)
                    continue;
                if (!exclusionPredicate.test(item))
                    items.add(item);
            }
            items = new ReferenceArrayList<>(new ReferenceLinkedOpenHashSet<>(items));
            return items;
        }

        private List<Item> collectItems(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Item, Item> entry : CROWNS.REGISTRATE.getAll(Registries.ITEM)) {

                Item item = entry.get();
                if (item instanceof BlockItem)
                    continue;
                if (!exclusionPredicate.test(item))
                    items.add(item);
            }
            return items;
        }

        private static void applyOrderings(List<Item> items, List<RegistrateDisplayItemsGenerator.ItemOrdering> orderings) {
            for (RegistrateDisplayItemsGenerator.ItemOrdering ordering : orderings) {
                int anchorIndex = items.indexOf(ordering.anchor());
                if (anchorIndex != -1) {
                    Item item = ordering.item();
                    int itemIndex = items.indexOf(item);
                    if (itemIndex != -1) {
                        items.remove(itemIndex);
                        if (itemIndex < anchorIndex) {
                            anchorIndex--;
                        }
                    }
                    if (ordering.type() == RegistrateDisplayItemsGenerator.ItemOrdering.Type.AFTER) {
                        items.add(anchorIndex + 1, item);
                    } else {
                        items.add(anchorIndex, item);
                    }
                }
            }
        }

        private static void outputAll(CreativeModeTab.Output output, List<Item> items, Function<Item, ItemStack> stackFunc, Function<Item, CreativeModeTab.TabVisibility> visibilityFunc) {
            for (Item item : items) {
                output.accept(stackFunc.apply(item), visibilityFunc.apply(item));
            }
        }

        private record ItemOrdering(Item item, Item anchor, RegistrateDisplayItemsGenerator.ItemOrdering.Type type) {
            public static RegistrateDisplayItemsGenerator.ItemOrdering before(Item item, Item anchor) {
                return new RegistrateDisplayItemsGenerator.ItemOrdering(item, anchor, RegistrateDisplayItemsGenerator.ItemOrdering.Type.BEFORE);
            }

            public static RegistrateDisplayItemsGenerator.ItemOrdering after(Item item, Item anchor) {
                return new RegistrateDisplayItemsGenerator.ItemOrdering(item, anchor, RegistrateDisplayItemsGenerator.ItemOrdering.Type.AFTER);
            }

            public enum Type {
                BEFORE,
                AFTER;
            }
        }
    }

}
