package com.rae.crowns.mixin;

import com.rae.crowns.CROWNSLang;
import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CreateLang.class)
public class LangMixin {
    @Inject(method = "fluidName", at = @At(value = "RETURN"), cancellable = true, remap = false)
    private static void addWaterStateInfo(FluidStack stack, CallbackInfoReturnable<LangBuilder> cir) {
        SpecificRealGazState newState = stack.get(DataComponentsInit.REAL_GAZ_STATE);
        if (newState != null) {
            cir.setReturnValue(cir.getReturnValue().add(CROWNSLang.specificRealFluidState(newState)));
        }

    }
}
