package com.rae.crowns;

import com.rae.crowns.config.CROWNSCfgClient;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.nuclear.Nucleus;
import com.rae.formicapi.FormicApiLang;
import com.rae.formicapi.content.thermal_utilities.FullTableBased;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class CROWNSLang extends Lang {
    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static LangBuilder builder() {
        return new LangBuilder(CROWNS.MODID);
    }

    public static LangBuilder text(String text) {
        return builder().text(text);
    }

    public static LangBuilder specificRealFluidState(SpecificRealGasState state) {
        CROWNSCfgClient.FluidVisualMode mode = CROWNSConfigs.CLIENT.fluidStateVisualMode.get();

        return switch (mode) {
            case TPX -> builder().add(Component.literal(" ")).add(
                    FormicApiLang.formatTemperature(state.temperature()).component()
                            .append(" | ")
                            .append(FormicApiLang.formatPressure(state.pressure()).component())
                            .append(" | ")
                            .append(
                                    Component.literal("x = " + LangNumberFormat.format(state.vaporQuality() * 100) + "%")
                            ));
            case PH -> builder().add(Component.literal(" ")).add(
                    FormicApiLang.formatPressure(state.pressure()).component()
                            .append(" | ")
                            .append(FormicApiLang.numberWithSymbol(state.specificEnthalpy()).text("J/Kg").component()));
            case PS -> builder().add(Component.literal(" ")).add(
                    FormicApiLang.formatPressure(state.pressure()).component()
                            .append(" | ")
                            .append(FormicApiLang.numberWithSymbol(FullTableBased.getS(state.specificEnthalpy(), state.pressure())).text("J/Kg/K").component()));
            case PHTSX -> builder().add(Component.literal(" ")).add(
                    FormicApiLang.formatPressure(state.pressure()).component()
                            .append(" | ")
                            .append(FormicApiLang.numberWithSymbol(state.specificEnthalpy()).text("J/Kg").component())
                            .append(" | ")
                            .append(FormicApiLang.formatTemperature(state.temperature()).component())
                            .append(" | ")
                            .append(FormicApiLang.numberWithSymbol(FullTableBased.getS(state.specificEnthalpy(), state.pressure())).text("J/Kg/K").component())
                            .append(" | ")
                            .append(
                                    Component.literal("x = " + LangNumberFormat.format(state.vaporQuality() * 100) + "%")));
        };
    }

    public static LangBuilder readableNucleus(Nucleus nucleus){
        String langKey = "nucleus." + nucleus.getId();
        return builder().translate(langKey, nucleus.getAtomicNumber());
    }

    public static LangBuilder nucleus(Nucleus nucleus){
        String langKey = "nucleus."+nucleus.getAtomicNumber();
        return builder().translate(langKey, nucleus.getAtomicMass());
    }
}
