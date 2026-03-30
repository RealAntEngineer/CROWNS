package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import com.simibubi.create.AllTags;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@SuppressWarnings("ALL")
public class TagsInit extends AllTags {

    public static void init() {
        CustomBlockTags.init();
        CustomItemTags.init();
        CustomEntityTag.init();
        CustomFluidTags.init();
    }

    public enum CustomNameSpace {

        MOD(CROWNS.MODID, false, true),
        COMMUN("c"),
        TIC("tconstruct"),
        QUARK("quark");

        public final String  id;
        public final boolean optionalDefault;
        public final boolean alwaysDatagenDefault;

        CustomNameSpace(String id) {
            this(id, true, false);
        }

        CustomNameSpace(String id, boolean optionalDefault, boolean alwaysDatagenDefault) {
            this.id = id;
            this.optionalDefault = optionalDefault;
            this.alwaysDatagenDefault = alwaysDatagenDefault;
        }
    }

    public enum CustomBlockTags {
        //TODO this is poor coding, use a float map instead.
        GOLD_BLOCK(CustomNameSpace.COMMUN, "storage_blocks/gold"),
        COAL_BLOCK(CustomNameSpace.COMMUN, "storage_blocks/coal"),
        REFACTORY_BLOCK(CustomNameSpace.MOD, "nuclear/refactory"),
        UNDESTRUCTABLE(CustomNameSpace.MOD, "nuclear/undestructable");

        public final TagKey<Block> tag;
        public final boolean       alwaysDatagen;

        CustomBlockTags() {
            this(CustomNameSpace.MOD);
        }

        CustomBlockTags(CustomNameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CustomBlockTags(CustomNameSpace namespace, boolean optional, boolean alwaysDatagen) {
            this(namespace, null, optional, alwaysDatagen);
        }

        CustomBlockTags(CustomNameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            if (optional) {
                tag = optionalTag(BuiltInRegistries.BLOCK, id);
            } else {
                tag = BlockTags.create(id);
            }
            this.alwaysDatagen = alwaysDatagen;
        }

        CustomBlockTags(CustomNameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        private static void init() {
        }

        public boolean matches(ItemStack stack) {
            return stack != null && stack.getItem() instanceof BlockItem blockItem && matches(blockItem.getBlock());
        }

        @SuppressWarnings("deprecation")
        public boolean matches(Block block) {
            return block.builtInRegistryHolder()
                    .is(tag);
        }

        public boolean matches(BlockState state) {
            return state.is(tag);
        }

    }

    public enum CustomItemTags {
        ;

        public final TagKey<Item> tag;
        public final boolean      alwaysDatagen;

        CustomItemTags() {
            this(CustomNameSpace.MOD);
        }

        CustomItemTags(CustomNameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CustomItemTags(CustomNameSpace namespace, boolean optional, boolean alwaysDatagen) {
            this(namespace, null, optional, alwaysDatagen);
        }

        CustomItemTags(CustomNameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            if (optional) {
                tag = optionalTag(BuiltInRegistries.ITEM, id);
            } else {
                tag = ItemTags.create(id);
            }
            this.alwaysDatagen = alwaysDatagen;
        }

        CustomItemTags(String path) {
            this(CustomNameSpace.MOD, path);
        }

        CustomItemTags(CustomNameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        private static void init() {
        }

        @SuppressWarnings("deprecation")
        public boolean matches(Item item) {
            return item.builtInRegistryHolder()
                    .is(tag);
        }

        public boolean matches(ItemStack stack) {
            return stack.is(tag);
        }

    }

    public enum CustomEntityTag {
        ;

        public final TagKey<EntityType<?>> tag;
        public final boolean               alwaysDatagen;

        CustomEntityTag() {
            this(CustomNameSpace.MOD);
        }

        CustomEntityTag(CustomNameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CustomEntityTag(CustomNameSpace namespace, boolean optional, boolean alwaysDatagen) {
            this(namespace, null, optional, alwaysDatagen);
        }

        CustomEntityTag(CustomNameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            if (optional) {
                tag = optionalTag(BuiltInRegistries.ENTITY_TYPE, id);
            } else {
                tag = TagKey.create(Registries.ENTITY_TYPE, id);
            }
            this.alwaysDatagen = alwaysDatagen;
        }

        CustomEntityTag(CustomNameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        private static void init() {
        }

        public boolean matches(Entity entity) {
            return entity.getType()
                    .is(tag);
        }

    }

    public enum CustomFluidTags {
        ;

        public final TagKey<Fluid> tag;
        public final boolean       alwaysDatagen;

        CustomFluidTags() {
            this(CustomNameSpace.MOD);
        }

        CustomFluidTags(CustomNameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        CustomFluidTags(CustomNameSpace namespace, boolean optional, boolean alwaysDatagen) {
            this(namespace, null, optional, alwaysDatagen);
        }

        CustomFluidTags(CustomNameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            if (optional) {
                tag = optionalTag(BuiltInRegistries.FLUID, id);
            } else {
                tag = FluidTags.create(id);
            }
            this.alwaysDatagen = alwaysDatagen;
        }

        CustomFluidTags(CustomNameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
        }

        private static void init() {
        }

        @SuppressWarnings("deprecation")
        public boolean matches(Fluid fluid) {
            return fluid.is(tag);
        }

        public boolean matches(FluidState state) {
            return state.is(tag);
        }

    }
}
