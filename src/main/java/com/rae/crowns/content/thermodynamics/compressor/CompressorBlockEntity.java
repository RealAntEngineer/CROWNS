package com.rae.crowns.content.thermodynamics.compressor;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
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

public class CompressorBlockEntity extends KineticBlockEntity {
    float power;
    protected LazyOptional<IFluidHandler> inputFluidCapability;
    protected LazyOptional<IFluidHandler> outputFluidCapability;

    //for later maybe ? to make the code simpler to understand
    private final StateFluidTank INPUT_WATER_TANK = new StateFluidTank(1000, (f)-> {
        setChanged();
    }){
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };
    private final StateFluidTank OUTPUT_WATER_TANK = new StateFluidTank(1000, (f)-> {
        setChanged();
    }){
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };
    public CompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputFluidCapability = LazyOptional.of(() -> INPUT_WATER_TANK);
        outputFluidCapability = LazyOptional.of(() -> OUTPUT_WATER_TANK);
    }
    @Override
    public float calculateStressApplied() {
        float combinedStress = getCombinedStress();
        this.lastStressApplied = combinedStress;
        return combinedStress;
    }
    //it's the base.
    private float getCombinedStress() {
        if (level == null) return 0;
        return -power/speed;// ? it's weird to do that but...
    }

    public float pressureRatio() {
        //depend on speed ?
        return 10;
    }
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, inputFluidCapability);
        return true;
    }
    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("input_water_tank", INPUT_WATER_TANK.writeToNBT(new CompoundTag()));
        tag.put("output_water_tank", OUTPUT_WATER_TANK.writeToNBT(new CompoundTag()));

    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        INPUT_WATER_TANK.readFromNBT((CompoundTag) tag.get("input_water_tank"));
        OUTPUT_WATER_TANK.readFromNBT((CompoundTag) tag.get("output_water_tank"));

        super.read(tag, clientPacket);
    }
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            Direction localDir = this.getBlockState().getValue(DirectionalBlock.FACING);
            if (side == localDir){
                return this.outputFluidCapability.cast();
            }
            if (side ==  localDir.getOpposite()){
                return this.inputFluidCapability.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    //nope -> we're gonna do that an other way : speed will fix flow and pressure is fixed
    // it's directional

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
    //really heavy -> to optimise and run less by second
    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;
    //make 2 tanks ?
    @Override
    public void tick() {
        super.tick();
        assert level != null;
        if (!level.isClientSide()) {
            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && queuedSync)
                    sendData();
            }
            SpecificRealGazState inputState =  INPUT_WATER_TANK.getState();
            FluidStack water = INPUT_WATER_TANK.drain((int) speed, IFluidHandler.FluidAction.SIMULATE);
            if(!water.isEmpty()) {
                SpecificRealGazState outputState = WaterAsRealGazTransformationHelper.standardCompression(inputState, 10);
                power = (outputState.specificEnthalpy() - inputState.specificEnthalpy()) * water.getAmount();
                CompoundTag tag = new CompoundTag();
                tag.put("realGazState", outputState.serialize());
                water.setTag(tag);
                INPUT_WATER_TANK.drain(Math.min((int) speed,OUTPUT_WATER_TANK.fill(water, IFluidHandler.FluidAction.EXECUTE)), IFluidHandler.FluidAction.EXECUTE);
                sendData();
            }
        }
    }
    @Override
    public void lazyTick() {
        super.lazyTick();

    }

}
