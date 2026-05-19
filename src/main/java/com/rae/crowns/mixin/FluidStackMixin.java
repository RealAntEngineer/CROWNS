package com.rae.crowns.mixin;

import com.rae.crowns.init.data.DataComponentsInit;
import net.minecraft.core.component.DataComponentPatch;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidStack.class)
public class FluidStackMixin {


    @Inject(method = "isSameFluidSameComponents", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private static void componentIsEqualForState(FluidStack first, FluidStack second, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {

            // Get component patches without realGazState
            DataComponentPatch firstPatch = first.copy().getComponentsPatch().forget((p) ->
                    p.equals(DataComponentsInit.REAL_GAS_STATE));
            DataComponentPatch secondPatch = second.copy().getComponentsPatch().forget((p) ->
                    p.equals(DataComponentsInit.REAL_GAS_STATE));
            boolean flag = firstPatch.equals(secondPatch);
            cir.setReturnValue(flag && first.is(second.getFluid()));
        }
    }
}
