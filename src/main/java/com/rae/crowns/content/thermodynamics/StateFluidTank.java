package com.rae.crowns.content.thermodynamics;

import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.rae.formicapi.thermal_utilities.helper.WaterCubicEOS;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.rae.formicapi.thermal_utilities.helper.WaterCubicEOS.DEFAULT_STATE;


public class StateFluidTank extends SmartFluidTank {
    public StateFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
        super(capacity, updateCallback);
    }
    public void heat(float amount){
        if (fluid.getAmount() > 0) {

            CompoundTag tag = new CompoundTag();
            CompoundTag oldStateNBT = fluid.getChildTag("realGazState");
            SpecificRealGazState oldState;
            if (oldStateNBT != null) {
                oldState = new SpecificRealGazState(oldStateNBT);
            } else {
                oldState = DEFAULT_STATE;
            }
            SpecificRealGazState state = WaterCubicEOS.isobaricTransfer(oldState, amount / getFluidAmount());
            tag.put("realGazState", state.serialize());
            fluid.setTag(tag);
        }
    }
    public void compress(float ratio){
        if (fluid.getAmount() > 0) {

            CompoundTag tag = new CompoundTag();
            CompoundTag oldStateNBT = fluid.getChildTag("realGazState");
            SpecificRealGazState oldState;
            if (oldStateNBT != null) {
                oldState = new SpecificRealGazState(oldStateNBT);
            } else {
                oldState = DEFAULT_STATE;
            }
            SpecificRealGazState state = WaterCubicEOS.isentropicCompression(oldState, ratio);
            tag.put("realGazState", state.serialize());
            fluid.setTag(tag);
        }
    }

    public SpecificRealGazState getState(){
        CompoundTag oldStateNBT = fluid.getChildTag("realGazState");
        SpecificRealGazState oldState;
        if (oldStateNBT!=null){
            oldState = new SpecificRealGazState(oldStateNBT);
        } else {
            oldState = DEFAULT_STATE;
        }
        return oldState;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack stack = super.drain(maxDrain, action);
        return stack;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return super.drain(resource, action);
    }


}
