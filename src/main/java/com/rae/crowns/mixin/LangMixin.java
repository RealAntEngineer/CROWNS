package com.rae.crowns.mixin;

import com.rae.crowns.CROWNSLang;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CreateLang.class)
public class LangMixin {
    @Inject(method = "fluidName", at = @At(value = "RETURN"), cancellable = true, remap = false)
    private static void addWaterStateInfo(@NotNull FluidStack stack, @NotNull CallbackInfoReturnable<LangBuilder> cir) {
        CompoundTag newStateNBT = stack.getChildTag("realGazState");
        if (newStateNBT != null && !newStateNBT.isEmpty()) {
            SpecificRealGazState newState = new SpecificRealGazState(newStateNBT);
            cir.setReturnValue(cir.getReturnValue().add(CROWNSLang.specificRealFluidState(newState)));
        }

    }
}
