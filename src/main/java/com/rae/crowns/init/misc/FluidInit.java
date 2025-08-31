package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.nuclear.corium.CoriumFluid;
import com.rae.crowns.content.nuclear.corium.CoriumLiquidBlock;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;


import java.util.function.Consumer;

public class FluidInit {
    private static final DeferredRegister<Fluid> FLUID_REGISTER =
            DeferredRegister.create(Registries.FLUID, CROWNS.MODID);
    public static final FluidEntry<CoriumFluid.Flowing> CORIUM =
            CROWNS.REGISTRATE.fluid("corium" ,CROWNS.resource("fluid/corium_still"), CROWNS.resource("fluid/corium_flowing"),
                    FluidInit::defaultFluidType,
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



    private static FluidType defaultFluidType(FluidType.Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        return new FluidType(properties) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }
                });
            }
        };
    }
    public static void register() {

    }
}