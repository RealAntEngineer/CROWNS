package com.rae.crowns.content.nuclear.display;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ReactorMonitorBlock extends Block implements IBE<ReactorMonitorBE> {
    public ReactorMonitorBlock(@NotNull Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Class<ReactorMonitorBE> getBlockEntityClass() {
        return ReactorMonitorBE.class;
    }

    @Override
    public @NotNull BlockEntityType<? extends ReactorMonitorBE> getBlockEntityType() {
        return BlockEntityInit.REACTOR_MONITOR.get();
    }

    @Override
    public InteractionResult onBlockEntityUse(BlockGetter world, BlockPos pos, Function<ReactorMonitorBE, InteractionResult> action) {
        return IBE.super.onBlockEntityUse(world, pos, action);
    }
}
