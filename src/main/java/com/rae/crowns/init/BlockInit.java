package com.rae.crowns.init;

import com.rae.crowns.content.nuclear.AssemblyBlock;
import com.rae.crowns.content.legacy.TurbineBearingBlock;
import com.rae.crowns.content.legacy.TurbineBladeBlock;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlock;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlock;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlock;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockBehaviour;

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
            .properties(p-> p.noOcclusion())
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();

    /*public static final BlockEntry<TurbineBladeBlock> TURBINE_BLADE = REGISTRATE.block(
            "turbine_blade", TurbineBladeBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p-> p.noOcclusion())
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();*/

    public static final BlockEntry<SteamInputBlock> STEAM_INPUT = REGISTRATE.block(
                    "steam_input", SteamInputBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p-> p.noOcclusion())
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();
    /*public static final BlockEntry<SteamCollectorBlock> STEAM_COLLECTOR = REGISTRATE.block(
                    "steam_collector", SteamCollectorBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p-> p.noOcclusion())
            .item()
            .tab(()->NUCLEAR_TAB)
            .build()
            .register();*/
    /*public static final BlockEntry<TurbineBearingBlock> TURBINE_BEARING =
            REGISTRATE.block("turbine_bearing",TurbineBearingBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p-> p.noOcclusion())
                    .transform(BlockStressDefaults.setGeneratorSpeed(TurbineBearingBlock::getSpeedRange))
                    .transform(BlockStressDefaults.setCapacity(100))
                    .item()
                    .tab(()-> NUCLEAR_TAB)
                    .build()
                    .register();*/

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
    public static void register() {}

}
