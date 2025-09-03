package com.rae.crowns.mixin;

import com.rae.formicapi.FormicApiLang;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CreateLang.class)
public class LangMixin {
    @Inject(method = "fluidName",at = @At(value = "RETURN" ),cancellable = true, remap = false)
    private static void addWaterStateInfo(FluidStack stack, CallbackInfoReturnable<LangBuilder> cir){
        CompoundTag newStateNBT = stack.getChildTag("realGazState");
        if (newStateNBT != null && !newStateNBT.isEmpty()) {
            SpecificRealGazState newState = new SpecificRealGazState(newStateNBT);
            cir.setReturnValue(cir.getReturnValue().add(Component.literal(" ")).add(
                    FormicApiLang.formatTemperature(newState.temperature()).component()
                            .append( " | ")
                            .append(FormicApiLang.formatPressure(newState.pressure()).component())
                            .append(" | ")
                            .append(
                                    Component.literal("x = " +(int) (newState.vaporQuality() *100) + "%")
                            )));
        }

    }
}
