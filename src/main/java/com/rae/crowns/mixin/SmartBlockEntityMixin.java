package com.rae.crowns.mixin;

import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Automatically registers and unregisters block entities implementing
 * {@link IHaveTemperature} in {@link PhysicsWorldData}.
 * <p>
 * This mixin hooks into {@link SmartBlockEntity#initialize()} and
 * {@link SmartBlockEntity#destroy()} so that temperature-aware entities
 * automatically manage their presence in the temperature world data.
 * </p>
 */
@Mixin(SmartBlockEntity.class)
public abstract class SmartBlockEntityMixin {

    /**
     * Called after {@link SmartBlockEntity#initialize()}.
     * Registers temperature-aware entities into {@link PhysicsWorldData}.
     */
    @Inject(method = "initialize", at = @At("TAIL"), remap = false)
    private void onInitialize(CallbackInfo ci) {
        SmartBlockEntity self = (SmartBlockEntity)(Object)this;

        if (self instanceof IHaveTemperature ht && self.getLevel() instanceof ServerLevel serverLevel) {
            PhysicsWorldData data = PhysicsSaveManager.get(serverLevel);
            if (data != null) {
                data.putDynamic(self.getBlockPos(), ht);
            }
        }
    }

    /**
     * Called after {@link SmartBlockEntity#destroy()}.
     * Unregisters temperature-aware entities from {@link PhysicsWorldData}.
     */
    @Inject(method = "destroy", at = @At("TAIL"), remap = false)
    private void onDestroy(CallbackInfo ci) {
        SmartBlockEntity self = (SmartBlockEntity)(Object)this;

        if (self instanceof IHaveTemperature && self.getLevel() instanceof ServerLevel serverLevel) {
            PhysicsWorldData data = PhysicsSaveManager.get(serverLevel);
            if (data != null) {
                data.removeDynamic(self.getBlockPos());
            }
        }
    }
}
