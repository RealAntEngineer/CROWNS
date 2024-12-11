package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlock;
import com.rae.crowns.init.EntityInit;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@MethodsReturnNonnullByDefault
public class SteamInputBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	private final StateFluidTank WATER_TANK = new StateFluidTank(1000, (f)-> {
        assert level != null;
        if (!level.isClientSide) {
			sendData();
		}
	}){
		@Override
		public boolean isFluidValid(FluidStack stack) {
			return stack.getFluid().is(FluidTags.WATER);
		}
	};
	public SteamCurrent steamCurrent;
	protected int currentUpdateCooldown;
	protected boolean updateSteamFlow;
	protected LazyOptional<IFluidHandler> fluidCapability;

	public SteamInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		steamCurrent = null;
		updateSteamFlow = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

		fluidCapability = LazyOptional.of(() -> WATER_TANK);
	}
	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		WATER_TANK.readFromNBT((CompoundTag) compound.get("water_tank"));
		super.read(compound, clientPacket);

	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.put("water_tank",WATER_TANK.writeToNBT(new CompoundTag()));

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

			if (updateSteamFlow) {
				updateSteamFlow = false;
				if (steamCurrent != null && !steamCurrent.isAlive()) {
					steamCurrent = null;
				}
				if (steamCurrent == null) {
					Direction facing = getBlockState().getValue(SteamInputBlock.FACING);
					List<SteamCurrent> currents = level.getEntitiesOfClass(SteamCurrent.class, new AABB(getBlockPos().relative(facing)));
					if (currents.isEmpty()) {
						steamCurrent = new SteamCurrent(EntityInit.CURRENT_ENTITY.get(), level);
						steamCurrent.setPos(worldPosition.relative(facing).getX(), worldPosition.relative(facing).getY(), worldPosition.relative(facing).getZ());
						steamCurrent.setInputFluidState(WATER_TANK.getState());
						steamCurrent.setFlow(0);
						level.addFreshEntity(steamCurrent);
						BlockPos collectorPos = steamCurrent.initialize(worldPosition, facing, 16);
					} else {
						steamCurrent = currents.get(0);
					}
				}
				sendData();
			}
			if (steamCurrent != null && steamCurrent.isAlive()) {
				steamCurrent.setInputFluidState(WATER_TANK.getState());
				steamCurrent.setFlow(WATER_TANK.drain(10, IFluidHandler.FluidAction.EXECUTE).getAmount()*20);
			}
			sendData();
		}
	}
	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) {
			Direction localDir = this.getBlockState().getValue(HeatExchangerBlock.FACING);
			if (side ==  localDir.getOpposite()){
				return this.fluidCapability.cast();
			}
		}
		return super.getCapability(cap, side);
	}
	@Override
	public void destroy() {
		if (steamCurrent != null && steamCurrent.isAlive()){
			steamCurrent.kill();
		}
		super.destroy();
	}
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);

		return true;
	}
}
