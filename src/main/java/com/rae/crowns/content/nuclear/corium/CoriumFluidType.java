package com.rae.crowns.content.nuclear.corium;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class CoriumFluidType extends FluidType {
    public CoriumFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public int getLightLevel(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return super.getLightLevel(state, getter, pos);
    }
}
