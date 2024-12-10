package com.rae.crowns.mixin;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;

import static com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper.mix;
import static com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity.DEFAULT_STATE;

@Mixin(value = SmartFluidTank.class)
public abstract class SmartFluidTankMixin extends FluidTank {

    public SmartFluidTankMixin(int capacity) {
        super(capacity);
    }
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!fluid.isEmpty()) {
            CompoundTag newStateNBT = resource.getChildTag("realGazState");
            SpecificRealGazState newState;
            if (newStateNBT != null) {
                newState = new SpecificRealGazState(newStateNBT);
            } else {
                newState = DEFAULT_STATE;
            }
            CompoundTag oldStateNBT = fluid.getChildTag("realGazState");
            SpecificRealGazState oldState;
            if (oldStateNBT != null) {
                oldState = new SpecificRealGazState(oldStateNBT);
            } else {
                oldState = DEFAULT_STATE;
            }
            CompoundTag mergedTag = new CompoundTag();

            mergedTag.put("realGazState",
                    mix(newState, resource.getAmount(), oldState, getFluidAmount()).serialize()
            );
            fluid.setTag(mergedTag);

            resource.setTag(fluid.getTag());//to ensure correct merge
            System.out.println("coucou");
        }
        System.out.println("coucou2");
        return super.fill(resource, action);
    }
}
