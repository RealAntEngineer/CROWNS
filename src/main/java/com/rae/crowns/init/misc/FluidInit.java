package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.nuclear.corium.CoriumFluid;
import com.rae.crowns.content.nuclear.corium.CoriumFluidType;
import com.rae.crowns.content.nuclear.corium.CoriumLiquidBlock;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidInteractionRegistry;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public class FluidInit {
    public static final FluidEntry<CoriumFluid.Flowing> CORIUM =
            CROWNS.REGISTRATE.fluid("corium", CROWNS.resource("fluid/corium_still"), CROWNS.resource("fluid/corium_flowing"),
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
    public static final FluidEntry<ForgeFlowingFluid.Flowing> URANIUM_HEXAFLUORIDE =
            CROWNS.REGISTRATE.fluid("uranium_hexafluoride", CROWNS.resource("fluid/uranium_hexafluoride_still"), CROWNS.resource("fluid/uranium_hexafluoride_flowing"))
                    .lang("Uranium_Hexafluoride")
                    .properties(b -> b.viscosity(2000)
                            .density(1400))
                    .fluidProperties(p -> p.levelDecreasePerBlock(2)
                            .tickRate(25)
                            .slopeFindDistance(3)
                            .explosionResistance(100f))
                    .source(ForgeFlowingFluid.Source::new) // TODO: remove when Registrate fixes FluidBuilder
                    .bucket()
                    .build()
                    .register();

    private static final DeferredRegister<Fluid> FLUID_REGISTER =
            DeferredRegister.create(Registries.FLUID, CROWNS.MODID);

    private static @NotNull FluidType defaultFluidType(FluidType.@NotNull Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        return new FluidType(properties) {

        };
    }

    public static void register() {

    }

    public static void registerFluidInteractions() {
        FluidInteractionRegistry.addInteraction(ForgeMod.WATER_TYPE.get(), new FluidInteractionRegistry.InteractionInformation(
                CORIUM.get().getFluidType(),
                fluidState -> {
                        return BlockInit.SOLID_CORIUM.get().defaultBlockState();
                }
        ));

        FluidInteractionRegistry.addInteraction(CORIUM.get().getFluidType(), new FluidInteractionRegistry.InteractionInformation(
                ForgeMod.WATER_TYPE.get(),
                fluidState -> {
                    return BlockInit.SOLID_CORIUM.get().defaultBlockState();
                }
        ));
    }
}