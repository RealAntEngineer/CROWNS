package com.rae.crowns.content.nuclear.rod;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RodBlockEntity extends BaseRodContainer {


    boolean scheduleRemoval = false;
    public RodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setNeutronBaseProperties(0.0f, 0.0f, 0.0f);
    }

    int tickUntilRemoval = 3;

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        if (scheduleRemoval && rodContained == null){
            tickUntilRemoval--;
            if (tickUntilRemoval < 0) {
                if (level.getFluidState(getBlockPos()).is(FluidTags.WATER)){
                    level.setBlock(getBlockPos(), Blocks.WATER.defaultBlockState(), 3);
                } else {
                    level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } else if (rodContained != null){
            scheduleRemoval = false;
            tickUntilRemoval = 3;
        }

        /*
        if (getBlockState().getBlock() instanceof RodBlock rodBlock)
            rodContained = rodBlock;*/
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putBoolean("ScheduleRemoval", scheduleRemoval);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        scheduleRemoval = tag.getBoolean("ScheduleRemoval");
    }

    @Override
    public void setRod(RodBlock rod) {
        super.setRod(rod);
        assert level != null;
        if (!level.isClientSide) {
            //no schedule removal for next tick and truly remove only if we didn't get a new rod in the meantime.
            //assert level != null;
            //
            scheduleRemoval = rod == null;
        }
    }
}