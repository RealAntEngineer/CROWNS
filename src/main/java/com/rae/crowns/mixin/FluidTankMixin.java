package com.rae.crowns.mixin;

import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
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

import static com.rae.formicapi.content.thermal_utilities.FullTableBased.mix;
import static com.rae.formicapi.content.thermal_utilities.SpecificRealGasState.DEFAULT_STATE;


@Mixin(value = FluidTank.class)
public abstract class FluidTankMixin {
    @Shadow
    @NotNull
    protected FluidStack fluid;

    @Inject(method = "fill", at = @At(value = "RETURN"))
    public void mergeStateNBT(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (resource.is(FluidTags.WATER)) {
            SpecificRealGasState newState = resource.get(DataComponentsInit.REAL_GAS_STATE);
            boolean newHasData = true, oldHasData = true;
            if (newState == null) {
                newState = DEFAULT_STATE;
                newHasData = false;
            }
            SpecificRealGasState oldState = fluid.isEmpty() ? DEFAULT_STATE : fluid.get(DataComponentsInit.REAL_GAS_STATE);
            if (oldState == null) {
                oldState = DEFAULT_STATE;
                oldHasData = false;
            }
            if (newHasData || oldHasData) {
                fluid.set(DataComponentsInit.REAL_GAS_STATE, mix(newState, resource.getAmount(), oldState, getFluidAmount()));
            }
        }
    }

    @Shadow
    public abstract int getFluidAmount();
}