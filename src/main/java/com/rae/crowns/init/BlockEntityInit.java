package com.rae.crowns.init;

import com.rae.crowns.client.rendering.instance.TurbineStageInstance;
import com.rae.crowns.client.rendering.renderer.HeatExchangerRenderer;
import com.rae.crowns.client.rendering.renderer.TurbineStageRenderer;
import com.rae.crowns.content.nuclear.AssemblyBlockEntity;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlockEntity;
import com.rae.crowns.content.legacy.TurbineBearingBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlockEntity;
import com.simibubi.create.content.contraptions.bearing.BearingInstance;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.rae.crowns.CROWNS.REGISTRATE;

public class BlockEntityInit {
    public static final BlockEntityEntry<AssemblyBlockEntity> FUEL_ASSEMBLY = REGISTRATE
            .blockEntity("fuel_assembly", AssemblyBlockEntity::new)
            .validBlock(BlockInit.FUEL_ASSEMBLY)
            .register();

    public static final BlockEntityEntry<TurbineBearingBlockEntity> TURBINE_BEARING = REGISTRATE
            .blockEntity("turbine_bearing", TurbineBearingBlockEntity::new)
            .instance(() -> BearingInstance::new)
            .validBlock(BlockInit.TURBINE_BEARING)
            .renderer(() -> BearingRenderer::new)
            .register();
    public static final BlockEntityEntry<TurbineStageBlockEntity> TURBINE_STAGE = REGISTRATE
            .blockEntity("turbine_stage", TurbineStageBlockEntity::new)
            .instance(() -> TurbineStageInstance::new)//renderNormally to false to prevent block rendering
            .validBlock(BlockInit.TURBINE_STAGE)
            .renderer(() -> TurbineStageRenderer::new)
            .register();

    public static final BlockEntityEntry<SteamInputBlockEntity> STEAM_INPUT = REGISTRATE.blockEntity(
            "steam_input",SteamInputBlockEntity::new)
            .validBlock(BlockInit.STEAM_INPUT)
            .register();
    public static final BlockEntityEntry<SteamCollectorBlockEntity> STEAM_COLLECTOR = REGISTRATE.blockEntity(
                    "steam_collector", SteamCollectorBlockEntity::new)
            .validBlock(BlockInit.STEAM_COLLECTOR)
            .register();
    public static final BlockEntityEntry<HeatExchangerBlockEntity> HEAT_EXCHANGER = REGISTRATE.blockEntity(
                    "heat_exchanger",HeatExchangerBlockEntity::new)
            .renderer(() -> HeatExchangerRenderer::new)
            .validBlock(BlockInit.HEAT_EXCHANGER)
            .register();
    public static void register() {}

}
