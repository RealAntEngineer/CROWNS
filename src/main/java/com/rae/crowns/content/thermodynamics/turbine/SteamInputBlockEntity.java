package com.rae.crowns.content.thermodynamics.turbine;


import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.misc.BlockEntityInit;

import com.rae.formicapi.content.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.lwjgl.system.NonnullDefault;

import javax.annotation.Nullable;
import java.util.List;

@NonnullDefault
public class SteamInputBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private static final int          SYNC_RATE = 8;
    public @Nullable     SteamCurrent steamCurrent;
    protected            int          currentUpdateCooldown;
    protected            boolean      updateSteamFlow;
    protected            int          syncCooldown;
    protected            boolean      queuedSync;
    float flow;
    private final StateFluidTank WATER_TANK = new StateFluidTank(1000, (f) -> {
        if (!hasLevel()) {
            return;
        }
        assert level != null;
        if (!level.isClientSide) {
            flow = f.getAmount() + 1;
            sendData();
        }
    }) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };

    public SteamInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        steamCurrent = null;
        updateSteamFlow = true;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityInit.STEAM_INPUT.get(),
                (be, context) -> {
                    Direction localDir = be.getBlockState().getValue(DirectionalBlock.FACING);
                    if (context == localDir.getOpposite()) {
                        return be.WATER_TANK;
                    }
                    return null;
                }
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        if (!level.isClientSide) {
            if (currentUpdateCooldown-- <= 0) {
                currentUpdateCooldown = AllConfigs.server().kinetics.fanBlockCheckRate.get();
                updateSteamFlow = true;
            }
            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && queuedSync)
                    sendData();
            }
            if (updateSteamFlow) {
                updateSteamFlow = false;

                Direction          facing   = getBlockState().getValue(SteamInputBlock.FACING);
                List<SteamCurrent> currents = SteamFlowManager.getCurrentsInBounds((ServerLevel) level, new AABB(worldPosition.relative(facing)));
                if (currents.isEmpty()) {
                    steamCurrent = new SteamCurrent(worldPosition, facing, 16);
                    steamCurrent.setInputFluidState(WATER_TANK.getState());
                    steamCurrent.rebuild(level);
                    SteamFlowManager.addSteamCurrent((ServerLevel) level, steamCurrent);
                } else {
                    steamCurrent = currents.getFirst();
                    steamCurrent.setDirection(facing);
                    steamCurrent.setInputFluidState(WATER_TANK.getState());
                    steamCurrent.rebuild(level);
                }
            }
            if (steamCurrent != null) {
                steamCurrent.setInputFluidState(WATER_TANK.getState());
                flow = WATER_TANK.drain((int) flow, IFluidHandler.FluidAction.EXECUTE).getAmount();
                sendData();
            }
        }
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("water_tank", WATER_TANK.writeToNBT(registries, new CompoundTag()));
        compound.putFloat("flow", flow);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag fluidTag = (CompoundTag) compound.get("water_tank");
        WATER_TANK.readFromNBT(registries, fluidTag != null ? fluidTag : new CompoundTag());
        flow = compound.getFloat("flow");
        super.read(compound, registries, clientPacket);
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

    public SpecificRealGazState getState() {
        return WATER_TANK.getState();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, WATER_TANK);
        CreateLang.builder().add(
                Component.literal(" Flow = " + flow + "/ 1000")
        ).forGoggles(tooltip, 1);

        return true;
    }

    public float getFlow() {
        return flow;
    }
}
