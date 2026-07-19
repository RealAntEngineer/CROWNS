package com.rae.crowns.content.thermodynamics.turbine;


import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.CROWNSLang;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.lwjgl.system.NonnullDefault;

import javax.annotation.Nullable;
import java.util.List;

@NonnullDefault
public class SteamInputBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final  int          MAX_FLOW  = 256;
    private static final int          SYNC_RATE = 8;
    private final StateFluidTank WATER_TANK = new StateFluidTank(MAX_FLOW * 2, (f) -> {
    }) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };
    public @Nullable     SteamCurrent steamCurrent;
    protected            int          currentUpdateCooldown;
    protected            boolean      updateSteamFlow;
    protected            int          syncCooldown;
    protected            boolean      queuedSync;
    float flow;
    private @Nullable ScrollValueBehaviour flowGoal;

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
        this.flowGoal = new ScrollValueBehaviour(CROWNSLang.translate("steam_input.flow_goal").component(),
                this, new ValueBox());
        flowGoal.between(0, MAX_FLOW);
        flowGoal.value = 64;
        behaviours.add(this.flowGoal);
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
            float flowGoal = this.flowGoal != null ? this.flowGoal.value : 0;
            if (steamCurrent != null) {
                steamCurrent.setInputFluidState(WATER_TANK.getState());
                SteamCollectorBlockEntity steamCollector = steamCurrent.getCollector(level);
                if (steamCollector != null) {
                    try {
                        if (steamCurrent.getDirection().getOpposite() == steamCollector.getBlockState().getValue(SteamCollectorBlock.FACING)) {
                            FluidStack water = WATER_TANK.drain((int) flowGoal, IFluidHandler.FluidAction.SIMULATE);
                            water.set(DataComponentsInit.REAL_GAS_STATE, steamCurrent.getOutputFluidState());
                            float filledFlow = steamCollector.getTank().fill(water,
                                    IFluidHandler.FluidAction.EXECUTE);
                            if (CROWNSConfigs.SERVER.kinetics.overflowIgnored.get()){
                                flow = water.getAmount();//if we ignore the overflow, we keep the amount from the initial draining.
                            } else {
                                flow = filledFlow;
                            }
                            WATER_TANK.drain((int) filledFlow, IFluidHandler.FluidAction.EXECUTE);
                        }
                    } catch (Exception ignored) {
                    }
                } else {
                    flow = WATER_TANK.drain((int) flowGoal, IFluidHandler.FluidAction.EXECUTE).getAmount();
                }
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
        if (fluidTag != null && fluidTag.contains("Fluid"))
            WATER_TANK.readFromNBT(registries,  fluidTag);

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

    public SpecificRealGasState getState() {
        return WATER_TANK.getState();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, WATER_TANK);
        CROWNSLang.translate("steam_input.real_flow", flow, MAX_FLOW).forGoggles(tooltip, 1);
        return true;
    }

    public float getFlow() {
        return flow;
    }

    static class ValueBox extends ValueBoxTransform.Sided {
        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(DirectionalBlock.FACING);
            Vec3      vec    = VecHelper.voxelSpace(8f, 8f, 15.5f);

            vec = VecHelper.rotateCentered(vec, AngleHelper.horizontalAngle(getSide()), Direction.Axis.Y);
            vec = VecHelper.rotateCentered(vec, AngleHelper.verticalAngle(getSide()), Direction.Axis.X);
            vec = vec.subtract(Vec3.atLowerCornerOf(facing.getNormal())
                    .scale(2 / 16f));

            return vec;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.5);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
            Direction facing = state.getValue(DirectionalBlock.FACING);
            if (facing.getAxis() == Direction.Axis.Y)
                return;
            if (getSide() != Direction.UP)
                return;
            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = state.getValue(CreativeMotorBlock.FACING);
            if (facing.getAxis() != Direction.Axis.Y && direction == Direction.DOWN)
                return false;
            return direction.getAxis() != facing.getAxis();
        }
    }
}