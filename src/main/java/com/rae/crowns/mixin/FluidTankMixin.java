package com.rae.crowns.mixin;

import com.rae.colony_api.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.init.data.DataComponentsInit;

import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.rae.colony_api.thermal_utilities.WaterCubicEOSTransformationHelper.DEFAULT_STATE;
import static com.rae.colony_api.thermal_utilities.WaterCubicEOSTransformationHelper.mix;

@Mixin(value = FluidTank.class)
public abstract class FluidTankMixin {
    @Shadow @NotNull protected FluidStack fluid;

    @Shadow public abstract int getFluidAmount();

    @Inject(method = "fill", at = @At(value = "RETURN"))
    public void mergeStateNBT(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
           if (resource.is(FluidTags.WATER)) {
               SpecificRealGazState newState = resource.get(DataComponentsInit.REAL_GAZ_STATE);
               if (newState == null) {
                   newState = DEFAULT_STATE;
               }
               SpecificRealGazState oldState = fluid.isEmpty()?DEFAULT_STATE:fluid.get(DataComponentsInit.REAL_GAZ_STATE);
               if (oldState == null) {
                   oldState = DEFAULT_STATE;
               }
               fluid.set(DataComponentsInit.REAL_GAZ_STATE, mix(newState, resource.getAmount(), oldState, getFluidAmount()));
           }
    }
}
