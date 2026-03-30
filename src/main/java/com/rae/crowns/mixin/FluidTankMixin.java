package com.rae.crowns.mixin;

import com.rae.formicapi.thermal_utilities.FullTableBased;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidTank.class)
public abstract class FluidTankMixin {
    @Shadow(remap = false)
    @NotNull
    protected FluidStack fluid;

    @Inject(method = "fill", at = @At(value = "HEAD"), remap = false)
    public void mergeStateNBT(@NotNull FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (!fluid.isEmpty() && fluid.isFluidEqual(resource) && fluid.getFluid().is(FluidTags.WATER)) {
            CompoundTag oldStateNBT = fluid.getChildTag("realGazState");
            SpecificRealGazState oldState;
            if (oldStateNBT != null && !oldStateNBT.isEmpty()) {
                oldState = new SpecificRealGazState(oldStateNBT);
            } else {
                oldState = FullTableBased.DEFAULT_STATE;
            }

            CompoundTag newStateNBT = resource.getChildTag("realGazState");
            SpecificRealGazState newState;
            if (newStateNBT != null && !newStateNBT.isEmpty()) {
                newState = new SpecificRealGazState(newStateNBT);
            } else {
                newState = FullTableBased.DEFAULT_STATE;
            }
            //too much duplication it's unreadable.
            if (newStateNBT != null && !newStateNBT.isEmpty() || oldStateNBT != null && !oldStateNBT.isEmpty()) {

                CompoundTag mergedTag = fluid.getOrCreateTag();

                mergedTag.put("realGazState",
                        FullTableBased.mix(newState, resource.getAmount(), oldState, getFluidAmount()).serialize()
                );
                fluid.setTag(mergedTag);

                resource.setTag(fluid.getTag());//to ensure correct merge
            }
        }
    }

    @Shadow(remap = false)
    public abstract int getFluidAmount();
}
