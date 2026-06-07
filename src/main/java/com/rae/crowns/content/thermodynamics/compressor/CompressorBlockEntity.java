package com.rae.crowns.content.thermodynamics.compressor;

import com.rae.crowns.CROWNSLang;
import com.rae.crowns.Constants;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.formicapi.content.thermal_utilities.FullTableBased;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
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

public class CompressorBlockEntity extends KineticBlockEntity {
    //really heavy -> to optimize and run less by second
    private static final int SYNC_RATE = 8;
    //for later maybe ? to make the code simpler to understand
    private final StateFluidTank INPUT_WATER_TANK = new StateFluidTank(1000, (f) -> {
        setChanged();
    }) {
        @Override
        public boolean isFluidValid(@NotNull FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };
    private final StateFluidTank OUTPUT_WATER_TANK = new StateFluidTank(1000, (f) -> {
        setChanged();
    }) {
        @Override
        public boolean isFluidValid(@NotNull FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };
    protected LazyOptional<IFluidHandler> inputFluidCapability;
    protected LazyOptional<IFluidHandler> outputFluidCapability;
    protected int syncCooldown;
    protected boolean queuedSync;
    float power;

    public CompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            Direction localDir = this.getBlockState().getValue(DirectionalBlock.FACING);
            if (side == localDir) {
                return this.outputFluidCapability.cast();
            }
            if (side == localDir.getOpposite()) {
                return this.inputFluidCapability.cast();
            }
        }
        return super.getCapability(cap, side);
    }

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
            SpecificRealGasState inputState = INPUT_WATER_TANK.getState();
            int flow = (int) Math.abs(speed);
            FluidStack water = INPUT_WATER_TANK.drain(flow, IFluidHandler.FluidAction.SIMULATE);
            float yield = CROWNSConfigs.SERVER.kinetics.compressorIsentropicYield.getF();

            if (!water.isEmpty()) {
                //depend on speed ?

                float pressureDelta = getPressureDelta(speed);
                SpecificRealGasState outputState = FullTableBased.isentropicCompression(inputState, (inputState.pressure() + pressureDelta) / inputState.pressure());
                power = (int) ((outputState.specificEnthalpy() - inputState.specificEnthalpy()) * water.getAmount() * 20f / Constants.whatSU / yield);

                CompoundTag tag = new CompoundTag();
                tag.put("realGazState", outputState.serialize());
                water.setTag(tag);
                INPUT_WATER_TANK.drain(Math.min((int) Math.abs(speed), OUTPUT_WATER_TANK.fill(water, IFluidHandler.FluidAction.EXECUTE)), IFluidHandler.FluidAction.EXECUTE);
                if (hasNetwork() && speed != 0) {

                    KineticNetwork network = getOrCreateNetwork();
                    network.updateStressFor(this, calculateStressApplied());
                    network.updateStress();
                }
                notifyUpdate();
            }
        }
    }

    @Override
    public float calculateStressApplied() {
        float combinedStress = getCombinedStress();
        this.lastStressApplied = combinedStress;
        return combinedStress;
    }

    @Override
    protected void write(@NotNull CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("power", power);
        tag.put("input_water_tank", INPUT_WATER_TANK.writeToNBT(new CompoundTag()));
        tag.put("output_water_tank", OUTPUT_WATER_TANK.writeToNBT(new CompoundTag()));

    }

    //nope -> we're gonna do that an other way : speed will fix flow and pressure is fixed
    // it's directional

    @Override
    protected void read(@NotNull CompoundTag tag, boolean clientPacket) {
        power = tag.getFloat("power");
        INPUT_WATER_TANK.readFromNBT((CompoundTag) tag.get("input_water_tank"));
        OUTPUT_WATER_TANK.readFromNBT((CompoundTag) tag.get("output_water_tank"));

        super.read(tag, clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputFluidCapability = LazyOptional.of(() -> INPUT_WATER_TANK);
        outputFluidCapability = LazyOptional.of(() -> OUTPUT_WATER_TANK);
    }

    @Override
    public boolean addToGoggleTooltip(@NotNull List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        SpecificRealGasState inputState = INPUT_WATER_TANK.getState();
        CreateLang.builder().add(
                        Component.literal("input : ")
                                .append(
                                        CROWNSLang.specificRealFluidState(inputState).component()))
                .forGoggles(tooltip, 1);
        SpecificRealGasState outputState = OUTPUT_WATER_TANK.getState();
        CreateLang.builder().add(
                        Component.literal("output : ").append(
                                CROWNSLang.specificRealFluidState(outputState).component()))
                .forGoggles(tooltip, 1);
        return true;
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

    public static float getPressureDelta(float speed) {
        int flow = (int) Math.abs(speed);
        float speedRef = CROWNSConfigs.SERVER.kinetics.compressorSpeedRef.getF();
        float flowRef = CROWNSConfigs.SERVER.kinetics.compressorFlowRef.getF();
        float pRef = CROWNSConfigs.SERVER.kinetics.compressorPressureRef.getF();
        float pressureDelta = pRef * (Math.abs(speed) * Math.abs(speed) / (speedRef * speedRef)) * (1 - (flow / flowRef) * (flow / flowRef));
        return pressureDelta;
    }

    //it's the base.
    private float getCombinedStress() {
        if (level == null) return 0;
        return speed == 0 ? 0 : Math.abs(power / speed);// ? it's weird to do that but...
    }

}
