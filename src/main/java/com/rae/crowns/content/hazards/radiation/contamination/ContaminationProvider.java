package com.rae.crowns.content.hazards.radiation.contamination;

// Capabilities is so shitty

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

public class ContaminationProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<IContamination> capability = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final IContamination instance = new Contamination();
    private final LazyOptional<IContamination> optional = LazyOptional.of(() -> instance);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == capability ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("contamination", instance.getRads());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) { // It threw a tantrum because I wrote deserialise
        instance.setRads(compoundTag.getDouble("contamination"));
    }
}
