package com.rae.crowns.mixin;

import com.rae.crowns.init.DataComponentsInit;
import net.minecraft.core.component.DataComponentPatch;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidStack.class)
public class FluidStackMixin {

    @Inject(method = "matches", at = @At(value = "RETURN"),remap = false, cancellable = true)
    private static void tagIsEqualForState(FluidStack first, FluidStack second, CallbackInfoReturnable<Boolean> cir){
        if (!cir.getReturnValue()){

            // Get component patches
            DataComponentPatch firstPatch = first.copy().getComponentsPatch();
            DataComponentPatch secondPatch = second.copy().getComponentsPatch();

            // Remove the realGazState component
            firstPatch.forget((p) -> p.equals(DataComponentsInit.REAL_GAZ_STATE));
            secondPatch.forget((p) -> p.equals(DataComponentsInit.REAL_GAZ_STATE));


            cir.setReturnValue(firstPatch.equals(secondPatch));
        }
    }
}
