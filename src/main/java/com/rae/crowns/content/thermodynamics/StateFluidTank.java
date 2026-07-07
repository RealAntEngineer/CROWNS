package com.rae.crowns.content.thermodynamics;


import com.rae.formicapi.content.thermal_utilities.FullTableBased;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.system.NonnullDefault;

import java.util.function.Consumer;

import static com.rae.formicapi.content.thermal_utilities.SpecificRealGasState.DEFAULT_STATE;

@NonnullDefault
public class StateFluidTank extends SmartFluidTank {
    public StateFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
        super(capacity, updateCallback);
    }

    public void heat(float amount) {
        if (fluid.getAmount() > 0) {

            CompoundTag          tag         = new CompoundTag();
            CompoundTag          oldStateNBT = fluid.getChildTag("realGasState");
            SpecificRealGasState oldState;
            if (oldStateNBT != null) {
                oldState = new SpecificRealGasState(oldStateNBT);
            } else {
                oldState = DEFAULT_STATE;
            }
            SpecificRealGasState state = FullTableBased.isobaricTransfer(oldState, amount / getFluidAmount());
            tag.put("realGasState", state.serialize());
            fluid.setTag(tag);
        }
    }

    public void compress(float ratio) {
        if (fluid.getAmount() > 0) {

            CompoundTag          tag         = new CompoundTag();
            CompoundTag          oldStateNBT = fluid.getChildTag("realGasState");
            SpecificRealGasState oldState;
            if (oldStateNBT != null) {
                oldState = new SpecificRealGasState(oldStateNBT);
            } else {
                oldState = DEFAULT_STATE;
            }
            SpecificRealGasState state = FullTableBased.isentropicCompression(oldState, ratio);
            tag.put("realGasState", state.serialize());
            fluid.setTag(tag);
        }
    }

    public SpecificRealGasState getState() {
        CompoundTag          oldStateNBT = fluid.getChildTag("realGasState");
        SpecificRealGasState oldState;
        if (oldStateNBT != null) {
            oldState = new SpecificRealGasState(oldStateNBT);
        } else {
            oldState = DEFAULT_STATE;
        }
        return oldState;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return super.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack stack = super.drain(maxDrain, action);
        return stack;
    }


}
