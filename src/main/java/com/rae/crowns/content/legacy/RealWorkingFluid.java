package com.rae.crowns.content.legacy;

import com.simibubi.create.content.fluids.VirtualFluid;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.checkerframework.checker.units.qual.Temperature;

public class RealWorkingFluid extends VirtualFluid {
    //will not work -> a less possible state should be used
    //what will be used ? temperature seem better

    //only graphic -> temperature for glow, vapor quality for texture
    public static final EnumProperty<Temperature> TEMPERATURE = EnumProperty.create("temperature",Temperature.class); //T*10
    public static final EnumProperty<VaporQuality> VAPOR_QUALITY = EnumProperty.create("temperature",VaporQuality.class); //T*10
    public RealWorkingFluid(Properties properties) {
        super(properties);
        registerDefaultState(defaultFluidState()
                .setValue(TEMPERATURE,Temperature.COLD)
                .setValue(VAPOR_QUALITY,VaporQuality.MIXED));
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> stateBuilder) {
        stateBuilder.add(TEMPERATURE,VAPOR_QUALITY);
        super.createFluidStateDefinition(stateBuilder);
    }


    public enum Temperature implements StringRepresentable {
        COLD,WARM,HOT;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    public enum VaporQuality implements StringRepresentable {
        LIQUID, MIXED,GAZ;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }
}
