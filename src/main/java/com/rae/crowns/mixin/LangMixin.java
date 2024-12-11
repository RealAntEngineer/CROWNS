package com.rae.crowns.mixin;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Lang.class)
public class LangMixin {
    @Inject(method = "fluidName",at = @At(value = "RETURN" ),cancellable = true, remap = false)
    private static void addWaterStateInfo(FluidStack stack, CallbackInfoReturnable<LangBuilder> cir){
        CompoundTag newStateNBT = stack.getChildTag("realGazState");
        if (newStateNBT != null) {
            SpecificRealGazState newState = new SpecificRealGazState(newStateNBT);
            cir.setReturnValue(cir.getReturnValue().add(Component.literal(" T = " +newState.temperature() + "°K")));
        }

    }
}
