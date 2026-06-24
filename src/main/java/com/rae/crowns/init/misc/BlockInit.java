package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.corium.SolidCoriumBlock;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlock;
import com.rae.crowns.content.nuclear.rod.GraphiteSleeveBlock;
import com.rae.crowns.content.nuclear.rod.RodBlock;
import com.rae.crowns.content.nuclear.rod.RodDriverBlock;
import com.rae.crowns.content.nuclear.uranium.UraniumOreBlock;
import com.rae.crowns.content.thermodynamics.compressor.CompressorBlock;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlock;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlock;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlock;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.rae.crowns.init.client.PartialModelInit;
import com.rae.formicapi.content.multiblock.MBItem;
import com.rae.formicapi.content.multiblock.MBStructureBlock;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import org.jetbrains.annotations.NotNull;

import java.util.function.ToIntFunction;

import static com.rae.crowns.CROWNS.REGISTRATE;
import static com.simibubi.create.api.behaviour.display.DisplaySource.displaySource;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

@SuppressWarnings("unused")
public class BlockInit {

    public static final BlockEntry<RodBlock> BORON_ROD = REGISTRATE.block(
            "boron_rod", p -> new RodBlock(p, PartialModelInit.BORON_ROD,0.5f, 0.0f, 0.0f))
            .initialProperties(SharedProperties::softMetal)
            //.blockstate(BlockStateGen.axisBlockProvider(false))
            .blockstate(
                    (c, p) -> BlockStateGen.axisBlock(
                            c, p, state ->
                                    p.models().getExistingFile(p.modLoc("block/rods/boron_rod_block"))
                    )
            )
            .properties(p -> p.noOcclusion().dynamicShape())
            .item()
            .model((c, p) ->
                    p.withExistingParent(c.getName(), p.modLoc("block/rods/boron_rod")))
            .build()
            .register();

    public static final BlockEntry<RodBlock> GRAPHITE_ROD = REGISTRATE.block(
                    "graphite_rod", p -> new RodBlock(p, PartialModelInit.GRAPHITE_ROD,0.1f, 0.8f, 0.0f))
            .initialProperties(SharedProperties::softMetal)
            //.blockstate(BlockStateGen.axisBlockProvider(false))
            .blockstate(
                    (c, p) -> BlockStateGen.axisBlock(
                            c, p, state ->
                                    p.models().getExistingFile(p.modLoc("block/rods/graphite_rod_block"))
                    )
            )
            .properties(p -> p.noOcclusion().dynamicShape())
            .item()
            .model((c, p) ->
                    p.withExistingParent(c.getName(), p.modLoc("block/rods/graphite_rod")))
            .build()
            .register();

    public static final BlockEntry<RodDriverBlock> ROD_DRIVER = REGISTRATE.block(
            "rod_driver", RodDriverBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .transform(axeOrPickaxe())
            .blockstate(BlockStateGen.directionalAxisBlockProvider())
            .item()
            .model((c, p) ->
                    p.withExistingParent(c.getName(), p.modLoc("block/rod_driver/horizontal")))
            .build()
            .register();

    public static final BlockEntry<GraphiteSleeveBlock> GRAPHITE_SLEEVE = REGISTRATE.block(
            "graphite_sleeve", GraphiteSleeveBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate(BlockStateGen.axisBlockProvider(false))
            .item()
            .build()
            .register();

    public static final BlockEntry<HeatExchangerBlock> HEAT_EXCHANGER = REGISTRATE
            .block("heat_exchanger", HeatExchangerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .blockstate(BlockStateGen.directionalBlockProvider(false))
            .transform(displaySource(DisplaySourceInit.TEMPERATURE))
            .properties(BlockBehaviour.Properties::noOcclusion)
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamInputBlock> STEAM_INPUT = REGISTRATE.block(
                    "steam_input", SteamInputBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .blockstate(BlockStateGen.directionalBlockProvider(false))
            .properties(BlockBehaviour.Properties::noOcclusion)
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamCollectorBlock> STEAM_COLLECTOR = REGISTRATE.block(
                    "steam_collector", SteamCollectorBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .blockstate(BlockStateGen.directionalBlockProvider(false))
            .properties(BlockBehaviour.Properties::noOcclusion)
            .item()
            .build()
            .register();

    public static final BlockEntry<MBStructureBlock> TURBINE_STAGE_STRUCTURE =
            REGISTRATE.block("turbine_stage_structure", MBStructureBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .blockstate((ctx, prov) ->
                            prov.getVariantBuilder(ctx.getEntry())
                                    .forAllStates(
                                            s ->
                            ConfiguredModel.builder()
                            .modelFile(prov.models()
                                    .getExistingFile(prov.modLoc("block/turbine_stage/structure")))
                            .build()
                    ))
                    .properties(p -> p.noOcclusion().isViewBlocking( ($1, $2, $3) -> false))
                    .item()
                    .model((c, p) ->
                            p.withExistingParent(c.getName(), p.modLoc("block/turbine_stage/structure")))
                    .build()
                    .register();

    public static final BlockEntry<TurbineStageBlock> TURBINE_STAGE =
            REGISTRATE.block("turbine_stage", (p) -> new TurbineStageBlock(p, TURBINE_STAGE_STRUCTURE.get()))
                    .initialProperties(SharedProperties::softMetal)
                    .blockstate(BlockStateGen.directionalBlockProvider(true))
                    .properties(p -> p.noOcclusion().isViewBlocking( ($1, $2, $3) -> false))
                    .item(MBItem::new)
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<CompressorBlock> COMPRESSOR =
            REGISTRATE.block("compressor", CompressorBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .blockstate(BlockStateGen.directionalBlockProvider(true))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<AssemblyBlock> FUEL_ASSEMBLY = REGISTRATE
            .block("fuel_assembly", AssemblyBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .blockstate(
                    (c, p) -> BlockStateGen.axisBlock(
                            c, p, state ->
                                    p.models().getExistingFile(p.modLoc("block/fuel_assembly/" +
                                            state.getValue(AssemblyBlock.ACTIVITY).getSerializedName()))
                    )
            )
            /*.blockstate((c, p) -> p.getVariantBuilder(c.getEntry())
                    .forAllStates(state -> {
                        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
                        String activity = state.getValue(AssemblyBlock.ACTIVITY).getSerializedName();

                        if (axis == Direction.Axis.Y) {
                            return ConfiguredModel.builder()
                                    .modelFile(p.models().getExistingFile(p.modLoc("block/fuel_assembly/" + activity)))
                                    .build();
                        }

                        return ConfiguredModel.builder()
                                .modelFile(p.models().getExistingFile(p.modLoc("block/fuel_assembly/" + activity + "_horizontal")))
                                .rotationX(90)
                                .rotationY(axis == Direction.Axis.X ? 90 : 0)
                                .build();
                    }))*/
            .properties(p -> p.lightLevel((s) -> {
                switch (s.getValue(AssemblyBlock.ACTIVITY)) {
                    case NONE -> {return 0;}
                    case LOW -> {return 8;}
                    case HIGH -> {return 15;}
                }
                return 0;
            }))
            .transform(displaySource(DisplaySourceInit.ACTIVITY))
            .transform(displaySource(DisplaySourceInit.TEMPERATURE))
            .transform(displaySource(DisplaySourceInit.FULL_STACK))
            .item()
            .model((c, p) ->
                    p.withExistingParent(c.getName(), p.modLoc("block/fuel_assembly/none")))
            .build()
            .register();

    public static final BlockEntry<UraniumOreBlock> DEEP_URANIUM_ORE = REGISTRATE
            .block("deepslate_uranium_ore", UraniumOreBlock::new)
            .initialProperties(() -> Blocks.DEEPSLATE)
            .properties(p -> p.lightLevel(litBlockEmission(9)).strength(5.5F, 4.0F))
            .item()
            .build()
            .register();

    public static final BlockEntry<UraniumOreBlock> URANIUM_ORE = REGISTRATE
            .block("uranium_ore", UraniumOreBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.lightLevel(litBlockEmission(9)).strength(4, 4))
            .item()
            .build()
            .register();

    public static final BlockEntry<SolidCoriumBlock> SOLID_CORIUM = REGISTRATE
            .block("solid_corium", SolidCoriumBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.lightLevel((blockState) -> 9).strength(4, 4))
            .item()
            .build()
            .register();

    private static @NotNull ToIntFunction<BlockState> litBlockEmission(int lightLevel) {
        return (blockState) -> blockState.getValue(BlockStateProperties.LIT) ? lightLevel : 0;
    }

    public static void register() {
    }
}