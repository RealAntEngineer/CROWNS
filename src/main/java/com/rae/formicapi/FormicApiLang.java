package com.rae.formicapi;

import com.rae.formicapi.units.Pressure;
import com.rae.formicapi.units.RadiationFlux;
import com.rae.formicapi.units.Temperature;
import com.rae.formicapi.config.FormicAPIConfigs;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class FormicApiLang extends Lang {
    //blatant copy of CreateLang
    /**
     * legacy-ish. Use CROWNSLang.translate and other builder methods where possible
     */
    public static MutableComponent translateDirect(String key, Object... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(FormicAPI.MODID + "." + key, args1);
    }

    public static List<Component> translatedOptions(String prefix, String... keys) {
        List<Component> result = new ArrayList<>(keys.length);
        for (String key : keys)
            result.add(translate((prefix != null ? prefix + "." : "") + key).component());
        return result;
    }

//

    public static LangBuilder builder() {
        return new LangBuilder(FormicAPI.MODID);
    }

    public static LangBuilder blockName(BlockState state) {
        return builder().add(state.getBlock()
                .getName());
    }

    public static LangBuilder itemName(ItemStack stack) {
        return builder().add(stack.getHoverName()
                .copy());
    }

    public static LangBuilder fluidName(FluidStack stack) {
        return builder().add(stack.getDisplayName()
                .copy());
    }

    public static LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static LangBuilder text(String text) {
        return builder().text(text);
    }

    @Deprecated // Use while implementing and replace all references with Lang.translate
    public static LangBuilder temporaryText(String text) {
        return builder().text(text);
    }

    public static LangBuilder formatTemperature(float temperature) {
        Temperature unit = FormicAPIConfigs.CLIENT.units.temperature.get();
        return CreateLang.builder().add(Component.literal("T = "))
                .add(number(unit.convert(temperature)))
                .text(" ")
                .add(unit.getSymbol());
    }
    public static LangBuilder formatPressure(float pressure) {
        Pressure unit = FormicAPIConfigs.CLIENT.units.pressure.get();
        return CreateLang.builder().add(Component.literal("P = "))
                .add(number(unit.convert(pressure)))
                .text(" ")
                .add(unit.getSymbol());
    }
    public static LangBuilder formatRadiationFlux(float radiationFlux) {
        RadiationFlux unit = FormicAPIConfigs.CLIENT.units.radiationFlux.get();
        return CreateLang.builder().add(Component.literal("activity : "))
                .add(number(unit.convert(radiationFlux)))
                .text(" ")
                .add(unit.getSymbol());
    }

}
