package com.rae.crowns.mixin;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.api.units.Pressure;
import com.rae.crowns.api.units.Temperature;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.init.DataComponentsInit;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CreateLang.class)
public class LangMixin {
    @Inject(method = "fluidName",at = @At(value = "RETURN" ),cancellable = true, remap = false)
    private static void addWaterStateInfo(FluidStack stack, CallbackInfoReturnable<LangBuilder> cir){
        SpecificRealGazState newState = stack.get(DataComponentsInit.REAL_GAZ_STATE);
        if (newState != null) {
            Temperature temperatureUnit = CROWNSConfigs.CLIENT.units.temperature.get();
            Pressure pressureUnit = CROWNSConfigs.CLIENT.units.pressure.get();
            cir.setReturnValue(cir.getReturnValue().add(
                    Component.literal(" T = " + (int) temperatureUnit.convert(newState.temperature()) + temperatureUnit.getSymbol()+ " | ").append(
                            Component.literal("P = " + (int)pressureUnit.convert( newState.pressure()) + pressureUnit.getSymbol() + " | ")
                    )
                            .append(
                                    Component.literal("x = " +(int) (newState.vaporQuality() *100) + "%")
                            )));
        }

    }
}
