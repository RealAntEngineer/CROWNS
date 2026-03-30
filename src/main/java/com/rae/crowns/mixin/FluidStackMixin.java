package com.rae.crowns.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidStack.class)
public abstract class FluidStackMixin {
    @Inject(method = "isFluidStackTagEqual", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private void tagIsEqualForState(@NotNull FluidStack other, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {

            CompoundTag firstTag  = this.getOrCreateTag().copy();
            CompoundTag secondTag = other.getOrCreateTag().copy();
            firstTag.remove("realGazState");
            secondTag.remove("realGazState");

            cir.setReturnValue(firstTag.equals(secondTag));

        }
    }

    @Shadow(remap = false)
    public abstract CompoundTag getOrCreateTag();
}
