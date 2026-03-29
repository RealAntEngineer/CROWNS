package com.rae.crowns.content.thermodynamics;


import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.formicapi.thermal_utilities.FullTableBased;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.rae.formicapi.thermal_utilities.FullTableBased.DEFAULT_STATE;


public class StateFluidTank extends SmartFluidTank {
    public StateFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
        super(capacity, updateCallback);
    }

    public void heat(float amount) {
        if (fluid.getAmount() > 0) {

            SpecificRealGazState oldState = fluid.get(DataComponentsInit.REAL_GAZ_STATE);
            if (oldState == null) {
                oldState = DEFAULT_STATE;
            }
            SpecificRealGazState state = FullTableBased.isobaricTransfer(oldState, amount / getFluidAmount());
            fluid.set(DataComponentsInit.REAL_GAZ_STATE, state);
        }
    }

    public SpecificRealGazState getState() {
        SpecificRealGazState oldState = fluid.get(DataComponentsInit.REAL_GAZ_STATE);
        if (oldState == null) {
            oldState = DEFAULT_STATE;
        }
        return oldState;
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return super.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        FluidStack stack = super.drain(maxDrain, action);
        return stack;
    }


}
