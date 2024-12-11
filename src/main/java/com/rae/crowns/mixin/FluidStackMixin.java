package com.rae.crowns.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidStack.class)
public class FluidStackMixin {
    @Shadow private CompoundTag tag;

    @Inject(method = "isFluidStackTagEqual", at = @At(value = "RETURN"),remap = false, cancellable = true)
    private void tagIsEqualForState(FluidStack other, CallbackInfoReturnable<Boolean> cir){
        if (!cir.getReturnValue()){

            CompoundTag firstTag = tag!=null?tag.copy():new CompoundTag();
            CompoundTag secondTag = other.getTag();
            firstTag.remove("realGazState");
            if (secondTag!= null)
                secondTag.remove("realGazState");
            else {
                secondTag = new CompoundTag();
            }

            cir.setReturnValue(firstTag.equals(secondTag));

        }
    }
}
