package com.rae.crowns.content.thermodynamics.turbine;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.List;

@MethodsReturnNonnullByDefault
public class SteamCollectorBlockEntity extends SmartBlockEntity {

	private final FluidTank WATER_TANK = new FluidTank(4000){
		@Override
		protected void onContentsChanged() {
			sendData();
			super.onContentsChanged();
		}

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
	
	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);

	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
	}

	/*@Override
	public void tick() {
		super.tick();
		assert level != null;

		if (currentUpdateCooldown-- <= 0) {
			currentUpdateCooldown = AllConfigs.server().kinetics.fanBlockCheckRate.get();
			updateSteamFlow = true;
		}

		if (updateSteamFlow) {
			updateSteamFlow = false;
			if (steamCurrent != null && !steamCurrent.isAlive()){
				steamCurrent = null;
			}
			if (steamCurrent == null) {
				List<SteamCurrent> currents = level.getEntitiesOfClass(SteamCurrent.class, new AABB(getBlockPos().relative(getBlockState().getValue(SteamInputBlock.FACING))));
				if (currents.isEmpty()) {
					steamCurrent = new SteamCurrent(EntityInit.CURRENT_ENTITY.get(), level);
					level.addFreshEntity(steamCurrent);
					BlockPos collectorPos = steamCurrent.initialize(worldPosition, getBlockState().getValue(SteamInputBlock.FACING), 16);
				}
				else {
					steamCurrent = currents.get(0);
				}
			}
			sendData();
		}
	}

	@Override
	public void destroy() {
		if (steamCurrent != null && steamCurrent.isAlive()){
			steamCurrent.kill();
		}
		super.destroy();
	}

	 */
}
