package com.rae.crowns.mixin;

import com.rae.colony_api.thermal_utilities.SpecificRealGazState;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.rae.colony_api.thermal_utilities.WaterAsRealGazTransformationHelper.DEFAULT_STATE;
import static com.rae.colony_api.thermal_utilities.WaterAsRealGazTransformationHelper.mix;

@Mixin(value = FluidTank.class)
public abstract class FluidTankMixin {
    @Shadow(remap = false) @NotNull protected FluidStack fluid;

    @Shadow(remap = false) public abstract int getFluidAmount();

    @Inject(method = "fill", at = @At(value = "HEAD"),remap = false)
    public void mergeStateNBT(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (!fluid.isEmpty() && fluid.isFluidEqual(resource)) {
            CompoundTag newStateNBT = resource.getChildTag("realGazState");
            SpecificRealGazState newState;
            if (newStateNBT != null) {
                newState = new SpecificRealGazState(newStateNBT);
            } else {
                newState = DEFAULT_STATE;
            }
            CompoundTag oldStateNBT = fluid.getChildTag("realGazState");
            SpecificRealGazState oldState;
            if (oldStateNBT != null) {
                oldState = new SpecificRealGazState(oldStateNBT);
            } else {
                oldState = DEFAULT_STATE;
            }
            CompoundTag mergedTag = fluid.getOrCreateTag();

            mergedTag.put("realGazState",
                    mix(newState, resource.getAmount(), oldState, getFluidAmount()).serialize()
            );
            if (oldStateNBT == null && newStateNBT == null){
                return;
            }
            fluid.setTag(mergedTag);

            resource.setTag(fluid.getTag());//to ensure correct merge
        }
    }
}
