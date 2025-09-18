package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.formicapi.FormicApiLang;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.CROWNSLang;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.misc.EntityInit;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@MethodsReturnNonnullByDefault
public class SteamInputBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	private final StateFluidTank WATER_TANK = new StateFluidTank(1000, (f)-> {
		if (!hasLevel()){
			return;
		}
        assert level != null;
        if (!level.isClientSide) {
			flow = f.getAmount()+1;
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
	float flow;

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
		if (compound.contains("water_tank"))
			WATER_TANK.readFromNBT((CompoundTag) compound.get("water_tank"));
		flow = compound.getFloat("flow");
		super.read(compound, clientPacket);
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.put("water_tank",WATER_TANK.writeToNBT(new CompoundTag()));
		compound.putFloat("flow", flow);
	}
	private static final int SYNC_RATE = 8;
	protected int syncCooldown;
	protected boolean queuedSync;
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

				if (steamCurrent != null){
					Direction facing = getBlockState().getValue(SteamInputBlock.FACING);
					steamCurrent.setDirection(facing);
					steamCurrent.setInputFluidState(WATER_TANK.getState());
					steamCurrent.rebuild(level);
					//steamCurrent.initialize(worldPosition, facing, 16);
				}
				if (steamCurrent == null) {
					Direction facing = getBlockState().getValue(SteamInputBlock.FACING);
					List<SteamCurrent> currents = SteamFlowManager.getCurrentsInBounds(level.dimension().location(), new AABB(worldPosition.relative(facing)));
					if (currents.isEmpty()) {
						steamCurrent = new SteamCurrent(worldPosition, facing, 16);
						//steamCurrent.setPos(worldPosition.relative(facing).getX(), worldPosition.relative(facing).getY(), worldPosition.relative(facing).getZ());
						steamCurrent.setInputFluidState(WATER_TANK.getState());
						steamCurrent.rebuild(level);
						SteamFlowManager.addSteamCurrent(level.dimension().location(), steamCurrent);//level.addFreshEntity(steamCurrent);
						//steamCurrent.initialize(worldPosition, facing, 16);
					} else {
						steamCurrent = currents.get(0);
					}
				}
			}
			if (steamCurrent != null) {
				steamCurrent.setInputFluidState(WATER_TANK.getState());
				flow  = WATER_TANK.drain((int) flow, IFluidHandler.FluidAction.EXECUTE).getAmount();
				sendData();
			}
		}
	}

	public SpecificRealGazState getState(){
		return WATER_TANK.getState();
	}
	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) {
			Direction localDir = this.getBlockState().getValue(DirectionalBlock.FACING);
			if (side ==  localDir.getOpposite()){
				return this.fluidCapability.cast();
			}
		}
		return super.getCapability(cap, side);
	}
	@Override
	public void destroy() {
		super.destroy();
	}
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		SpecificRealGazState newState = getState();
		FormicApiLang.formatTemperature(newState.temperature())
				.text( " | ")
				.add(FormicApiLang.formatPressure(newState.pressure()).component())
				.text(" | ")
				.add(
						Component.literal("x = " +(int) (newState.vaporQuality() *100) + "%")
				)
				.forGoggles(tooltip, 1);
		CreateLang.builder().add(
				Component.literal(" Flow = "+ flow + "/ 1000")
		)				.forGoggles(tooltip, 1);

		return true;
	}

	public float getFlow() {
		return flow;
	}
}
