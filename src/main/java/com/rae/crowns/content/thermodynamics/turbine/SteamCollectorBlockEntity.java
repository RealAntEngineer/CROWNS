package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@MethodsReturnNonnullByDefault
public class SteamCollectorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private static final int SYNC_RATE = 8;
    //public SteamCurrent steamCurrent;
    //protected int currentUpdateCooldown;
    //protected boolean updateSteamFlow;
    protected LazyOptional<IFluidHandler> fluidCapability;
    protected int syncCooldown;
    protected boolean queuedSync;
    private final StateFluidTank WATER_TANK = new StateFluidTank(1000, (f) -> {
        if (!hasLevel()) {
            return;
        }
        assert level != null;
        if (!level.isClientSide) {
            sendData();
        }
    }) {
        @Override
        public boolean isFluidValid(@NotNull FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };

    public SteamCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        //steamCurrent = null;
        //updateSteamFlow = true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

        fluidCapability = LazyOptional.of(() -> WATER_TANK);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            Direction localDir = this.getBlockState().getValue(DirectionalBlock.FACING);
            if (side == localDir.getOpposite()) {
                return this.fluidCapability.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void read(@NotNull CompoundTag compound, boolean clientPacket) {
        if (compound.contains("water_tank"))
            WATER_TANK.readFromNBT((CompoundTag) compound.get("water_tank"));
        super.read(compound, clientPacket);
    }

    @Override
    public void write(@NotNull CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("water_tank", WATER_TANK.writeToNBT(new CompoundTag()));
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        if (!level.isClientSide) {
            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && queuedSync)
                    sendData();
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);

        return true;
    }


    public StateFluidTank getTank() {
        return WATER_TANK;
    }
}
