package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.colony_api.thermal_utilities.SpecificRealGazState;
import com.rae.colony_api.units.Pressure;
import com.rae.colony_api.units.Temperature;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.rae.crowns.init.misc.EntityInit;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

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
	float flow;

	public SteamInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		steamCurrent = null;
		updateSteamFlow = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

	}


	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		WATER_TANK.readFromNBT(registries,(CompoundTag) compound.get("water_tank"));
		flow = compound.getFloat("flow");
		super.read(compound, registries,clientPacket);
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries,  boolean clientPacket) {
		super.write(compound, registries,clientPacket);
		compound.put("water_tank",WATER_TANK.writeToNBT(registries,new CompoundTag()));
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
				if (steamCurrent != null && !steamCurrent.isAlive()) {
					steamCurrent = null;
				}
				else if (steamCurrent != null && steamCurrent.isAlive()){
					Direction facing = getBlockState().getValue(SteamInputBlock.FACING);
					steamCurrent.setPos(worldPosition.relative(facing).getX(), worldPosition.relative(facing).getY(), worldPosition.relative(facing).getZ());
					steamCurrent.setInputFluidState(WATER_TANK.getState());
					steamCurrent.initialize(worldPosition, facing, 16);
				}
				if (steamCurrent == null) {
					Direction facing = getBlockState().getValue(SteamInputBlock.FACING);
					List<SteamCurrent> currents = level.getEntitiesOfClass(SteamCurrent.class, new AABB(getBlockPos().relative(facing)));
					if (currents.isEmpty()) {
						steamCurrent = new SteamCurrent(EntityInit.CURRENT_ENTITY.get(), level);
						steamCurrent.setPos(worldPosition.relative(facing).getX(), worldPosition.relative(facing).getY(), worldPosition.relative(facing).getZ());
						steamCurrent.setInputFluidState(WATER_TANK.getState());
						level.addFreshEntity(steamCurrent);
						steamCurrent.initialize(worldPosition, facing, 16);
					} else {
						steamCurrent = currents.get(0);
					}
				}
			}
			if (steamCurrent != null && steamCurrent.isAlive()) {
				steamCurrent.setInputFluidState(WATER_TANK.getState());
				flow  = WATER_TANK.drain((int) flow, IFluidHandler.FluidAction.EXECUTE).getAmount();
				sendData();
			}
		}
	}

	public SpecificRealGazState getState(){
		return WATER_TANK.getState();
	}



	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.FluidHandler.BLOCK,
				BlockEntityInit.STEAM_INPUT.get(),
				(be, context) -> {
					Direction localDir = be.getBlockState().getValue(DirectionalBlock.FACING);
					if (context ==  localDir.getOpposite()){
						return be.WATER_TANK;
					}
					return null;
				}
		);
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
		SpecificRealGazState newState = getState();
		Temperature temperatureUnit = CROWNSConfigs.CLIENT.units.temperature.get();
		Pressure pressureUnit = CROWNSConfigs.CLIENT.units.pressure.get();
		CreateLang.builder().add(
				Component.literal(" T = " + (int) temperatureUnit.convert(newState.temperature()) + temperatureUnit.getSymbol()+ " | ").append(
								Component.literal("P = " + (int)pressureUnit.convert( newState.pressure()) + pressureUnit.getSymbol() + " | ")
						)
						.append(
								Component.literal("x = " +(int) (newState.vaporQuality() *100) + "%")
						))
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
