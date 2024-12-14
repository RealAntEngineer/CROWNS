package com.rae.crowns.init;

import com.rae.crowns.content.nuclear.AssemblyBlock;
import com.rae.crowns.content.legacy.TurbineBearingBlock;
import com.rae.crowns.content.legacy.TurbineBladeBlock;
import com.rae.crowns.content.nuclear.UraniumOreBlock;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlock;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlock;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlock;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.ToIntFunction;

import static com.rae.crowns.CROWNS.REGISTRATE;
import static com.rae.crowns.init.CreativeModeTabsInit.NUCLEAR_TAB;

public class BlockInit {

    //to do list -> uranium ore (enrichment ?) + plutonium (created from 235) + depletion of fuel
    // control bar
    // thermal exchanger pipe ( entry, exit and middle : fluid tanks on both side entry and exit -> flow rate ? pressure loss ?)

    // turbine contraption ? -> turbine blade model + entry and exit ports
    // compressor ?
    public static final BlockEntry<AssemblyBlock> FUEL_ASSEMBLY = REGISTRATE
            .block("fuel_assembly", AssemblyBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p-> p.lightLevel((s)-> {
                switch (s.getValue(AssemblyBlock.ACTIVITY)) {
                    case NONE -> {
                        return 0;
                    }
                    case LOW -> {
                        return 8;
                    }
                    case HIGH -> {
                        return 15;
                    }
                }
                return 0;
            }))
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();
    public static final BlockEntry<HeatExchangerBlock> HEAT_EXCHANGER = REGISTRATE
            .block("heat_exchanger", HeatExchangerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();

    public static final BlockEntry<UraniumOreBlock> URANIUM_ORE = REGISTRATE
            .block("uranium_ore", UraniumOreBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p->p.lightLevel(litBlockEmission(9)).strength(4,4))
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();
    public static final BlockEntry<UraniumOreBlock> DEEP_URANIUM_ORE = REGISTRATE
            .block("deepslate_uranium_ore", UraniumOreBlock::new)
            .initialProperties(()-> Blocks.DEEPSLATE)
            .properties(p->p.lightLevel(litBlockEmission(9)).strength(5.5F, 4.0F))
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();
    public static final BlockEntry<SteamInputBlock> STEAM_INPUT = REGISTRATE.block(
                    "steam_input", SteamInputBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();

    public static final BlockEntry<TurbineStageBlock> TURBINE_STAGE =
            REGISTRATE.block("turbine_stage",TurbineStageBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .transform(BlockStressDefaults.setGeneratorSpeed(TurbineStageBlock::getSpeedRange))
                    .transform(BlockStressDefaults.setCapacity(100))
                    .item()
                    .tab(()-> NUCLEAR_TAB)
                    .build()
                    .register();
    private static ToIntFunction<BlockState> litBlockEmission(int lightLevel) {
        return (blockState) -> blockState.getValue(BlockStateProperties.LIT) ? lightLevel : 0;
    }
    public static void register() {}

}
