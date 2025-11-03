package com.rae.crowns.mixin;

import com.rae.crowns.content.fields.temperature.TemperatureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {


    protected ServerLevelMixin(@NotNull WritableLevelData p_270739_, @NotNull ResourceKey<Level> p_270683_, @NotNull RegistryAccess p_270200_, @NotNull Holder<DimensionType> p_270240_, @NotNull Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
    }

    @Shadow
    public abstract ServerLevel getLevel();

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void onSetBlockState(@NotNull BlockPos pos, @NotNull BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (!oldState.equals(newState)) {
            TemperatureManager.get(getLevel()).registerChanged(pos.immutable());
        }
    }
}