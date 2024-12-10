package com.rae.crowns.content.thermodynamics.conduction;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity.DEFAULT_STATE;

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
            tag.put("realGazState", WaterAsRealGazTransformationHelper.isobaricTransfert(oldState, amount / getFluidAmount()).serialize());
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
