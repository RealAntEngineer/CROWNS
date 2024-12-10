package com.rae.crowns.mixin;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper.mix;
import static com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity.DEFAULT_STATE;

@Mixin(value = FluidTank.class)
public abstract class SmartFluidTankMixin  {
    @Shadow @NotNull protected FluidStack fluid;

    @Shadow public abstract int getFluidAmount();

    @Inject(method = "fill", at = @At(value = "HEAD"),remap = false)
    public void fill(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
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
        }
    }
}
