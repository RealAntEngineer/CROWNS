package com.rae.crowns.mixin;

import com.rae.colony_api.thermal_utilities.SpecificRealGazState;
import com.rae.colony_api.units.Pressure;
import com.rae.colony_api.units.Temperature;
import com.rae.crowns.config.CROWNSConfigs;
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
        if (newStateNBT != null) {
            SpecificRealGazState newState = new SpecificRealGazState(newStateNBT);
            Temperature temperatureUnit = CROWNSConfigs.CLIENT.units.temperature.get();
            Pressure pressureUnit = CROWNSConfigs.CLIENT.units.pressure.get();
            cir.setReturnValue(cir.getReturnValue().add(
                    Component.literal(" T = " + (int) temperatureUnit.convert(newState.temperature()) + temperatureUnit.getSymbol()+ " | ").append(
                            Component.literal(String.format("P = %.2f %s | ", pressureUnit.convert(newState.pressure()), pressureUnit.getSymbol()))                                )
                            .append(
                                    Component.literal("x = " +(int) (newState.vaporQuality() *100) + "%")
                            )));
        }

    }
}
