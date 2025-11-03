package com.rae.crowns;

import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CROWNSLang extends Lang {
    //blatant copy of CreateLang

    /**
     * legacy-ish. Use CROWNSLang.translate and other builder methods where possible
     */
    public static @NotNull MutableComponent translateDirect(String key, Object @NotNull ... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(CROWNS.MODID + "." + key, args1);
    }

    public static @NotNull List<Component> translatedOptions(@Nullable String prefix, String @NotNull ... keys) {
        List<Component> result = new ArrayList<>(keys.length);
        for (String key : keys)
            result.add(translate((prefix != null ? prefix + "." : "") + key).component());
        return result;
    }

//

    public static @NotNull LangBuilder builder() {
        return new LangBuilder(CROWNS.MODID);
    }

    public static @NotNull LangBuilder blockName(@NotNull BlockState state) {
        return builder().add(state.getBlock()
                .getName());
    }

    public static @NotNull LangBuilder itemName(@NotNull ItemStack stack) {
        return builder().add(stack.getHoverName()
                .copy());
    }

    public static @NotNull LangBuilder fluidName(@NotNull FluidStack stack) {
        return builder().add(stack.getDisplayName()
                .copy());
    }

    public static @NotNull LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }

    public static @NotNull LangBuilder translate(@NotNull String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static @NotNull LangBuilder text(@NotNull String text) {
        return builder().text(text);
    }

    @Deprecated // Use while implementing and replace all references with Lang.translate
    public static @NotNull LangBuilder temporaryText(@NotNull String text) {
        return builder().text(text);
    }


}
