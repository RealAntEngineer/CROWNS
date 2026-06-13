package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.fuel_feeder.FuelFeederBlockEntity;
import com.rae.crowns.content.nuclear.fuel_rod.FuelRodBlockEntity;
import com.rae.crowns.content.thermodynamics.compressor.CompressorBlockEntity;
import com.rae.crowns.content.thermodynamics.compressor.CompressorRenderer;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerRenderer;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageRenderer;
import com.rae.crowns.init.client.PartialModelInit;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.rae.crowns.CROWNS.REGISTRATE;

@SuppressWarnings("ALL")
    public static final BlockEntityEntry<FuelRodBlockEntity> FUEL_ROD = REGISTRATE
            .blockEntity("fuel_rod", FuelRodBlockEntity::new)
            .validBlock(BlockInit.FUEL_ROD)
            .register();

    public static final BlockEntityEntry<FuelFeederBlockEntity> FUEL_FEEDER = REGISTRATE
            .blockEntity("fuel_feeder", FuelFeederBlockEntity::new)
            .validBlock(BlockInit.FUEL_FEEDER)
            .register();

    public static final BlockEntityEntry<TurbineStageBlockEntity> TURBINE_STAGE = REGISTRATE
            .blockEntity("turbine_stage", TurbineStageBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual.ofZ(PartialModelInit.TURBINE_STAGE))
            .validBlock(BlockInit.TURBINE_STAGE)
            .renderer(() -> TurbineStageRenderer::new)
            .register();

    public static final BlockEntityEntry<CompressorBlockEntity> COMPRESSOR = REGISTRATE
            .blockEntity("compressor_stage", CompressorBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual.ofZ(AllPartialModels.MECHANICAL_PUMP_COG))
            .validBlock(BlockInit.COMPRESSOR)
            .renderer(() -> CompressorRenderer::new)
            .register();

    public static final BlockEntityEntry<SteamInputBlockEntity> STEAM_INPUT = REGISTRATE.blockEntity(
                    "steam_input", SteamInputBlockEntity::new)
            .validBlock(BlockInit.STEAM_INPUT)
            .register();

    public static final BlockEntityEntry<SteamCollectorBlockEntity> STEAM_COLLECTOR = REGISTRATE.blockEntity(
                    "steam_collector", SteamCollectorBlockEntity::new)
            .validBlock(BlockInit.STEAM_COLLECTOR)
            .register();

    public static final BlockEntityEntry<HeatExchangerBlockEntity> HEAT_EXCHANGER = REGISTRATE.blockEntity(
                    "heat_exchanger", HeatExchangerBlockEntity::new)
            .renderer(() -> HeatExchangerRenderer::new)
            .validBlock(BlockInit.HEAT_EXCHANGER)
            .register();

    public static void register() {
    }

}
