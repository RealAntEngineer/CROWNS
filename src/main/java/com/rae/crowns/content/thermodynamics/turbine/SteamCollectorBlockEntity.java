package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.BlockEntityInit;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

@MethodsReturnNonnullByDefault
public class SteamCollectorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	private final StateFluidTank WATER_TANK =  new StateFluidTank(1000, (f)-> {
		if (!hasLevel()){
			return;
		}
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
	//public SteamCurrent steamCurrent;
	//protected int currentUpdateCooldown;
	//protected boolean updateSteamFlow;
	public SteamCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		//steamCurrent = null;
		//updateSteamFlow = true;
	}
	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}


	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.FluidHandler.BLOCK,
				BlockEntityInit.STEAM_COLLECTOR.get(),
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
	protected void read(CompoundTag compound, HolderLookup.Provider registries,  boolean clientPacket) {
		WATER_TANK.readFromNBT(registries,(CompoundTag) compound.get("water_tank"));
		super.read(compound, registries,clientPacket);
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries,  boolean clientPacket) {
		super.write(compound, registries,clientPacket);
		compound.put("water_tank",WATER_TANK.writeToNBT(registries,new CompoundTag()));
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
			if (syncCooldown > 0) {
				syncCooldown--;
				if (syncCooldown == 0 && queuedSync)
					sendData();
			}
		}
	}
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		containedFluidTooltip(tooltip, isPlayerSneaking, WATER_TANK);

		return true;
	}


}
