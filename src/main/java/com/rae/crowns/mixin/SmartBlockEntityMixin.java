package com.rae.crowns.mixin;

import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Automatically registers and unregisters block entities implementing
 * {@link IHaveTemperature} in {@link PhysicsWorldData}.
 */
@Mixin(SmartBlockEntity.class)
public abstract class SmartBlockEntityMixin {
    @Unique
    boolean cROWNS_1_20_1$registrationDone = false;
    /**
     * Called after {@link SmartBlockEntity#tick()}.
     * Registers temperature-aware entities into {@link PhysicsWorldData}.
     * only try to load if the section has gone through the read method
     */
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void onTick(CallbackInfo ci) {
        SmartBlockEntity self = (SmartBlockEntity)(Object)this;
        if (cROWNS_1_20_1$registrationDone) return;

        if (self instanceof IHaveTemperature ht && self.getLevel() instanceof ServerLevel serverLevel) {
            PhysicsWorldData data = PhysicsSaveManager.get(serverLevel);
            BlockPos pos = self.getBlockPos();
            SectionPos sectionPos = SectionPos.of(pos);
            if (data!=null && PhysicsSaveManager.isLoaded(serverLevel.dimension(),SectionPos.asLong(pos))) {
                data.putDynamic(self.getBlockPos(), ht);
                cROWNS_1_20_1$registrationDone = true;
            }
        } else cROWNS_1_20_1$registrationDone = true;
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
