package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.nuclear.corium.CoriumFluid;
import com.rae.crowns.content.nuclear.corium.CoriumFluidType;
import com.rae.crowns.content.nuclear.corium.CoriumLiquidBlock;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FluidInit {
    private static final DeferredRegister<Fluid> FLUID_REGISTER =
            DeferredRegister.create(Registries.FLUID, CROWNS.MODID);
    public static final FluidEntry<CoriumFluid.Flowing> CORIUM =
            CROWNS.REGISTRATE.fluid("corium" ,CROWNS.resource("fluid/corium_still"), CROWNS.resource("fluid/corium_flowing"),
                            CoriumFluidType::new,
                            CoriumFluid.Flowing::new)
                    .lang("Corium")
                    .properties(b -> b.viscosity(2000)
                            .density(1400))
                    .fluidProperties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .slopeFindDistance(3)
                            .explosionResistance(100f))
                    .source(CoriumFluid.Source::new)
                    .block(CoriumLiquidBlock::new)
                    .build()
                    .bucket()
                    .build()
                    .register();

    public static final FluidEntry<BaseFlowingFluid.Flowing> URANIUM_HEXAFLUOR =
            CROWNS.REGISTRATE.fluid("uranium_hexafluoride" ,CROWNS.resource("fluid/uranium_hexafluoride_still"), CROWNS.resource("fluid/uranium_hexafluoride_flowing"))
                    .lang("Uranium_Hexafluoride")
                    .properties(b -> b.viscosity(2000)
                            .density(1400))
                    .fluidProperties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .slopeFindDistance(3)
                            .explosionResistance(100f))
                    .source(BaseFlowingFluid.Source::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .build()
                    .register();

    public static void register() {

    }
}