package com.rae.crowns.content.thermodynamics.compressor;

import com.rae.colony_api.thermal_utilities.SpecificRealGazState;
import com.rae.colony_api.thermal_utilities.WaterCubicEOSTransformationHelper;
import com.rae.crowns.CROWNSLang;
import com.rae.crowns.Constants;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.rae.crowns.init.data.DataComponentsInit;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public class CompressorBlockEntity extends KineticBlockEntity {
    float power;

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
        return speed==0?0:Math.abs(power/speed);// ? it's weird to do that but...
    }
    //TODO use a config
    public float pressureRatio() {
        //depend on speed ?
        return 8;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityInit.COMPRESSOR.get(),
                (be, context) -> {
                    Direction localDir = be.getBlockState().getValue(DirectionalBlock.FACING);
                    if (context != null) {
                        if (localDir == context.getOpposite()) {
                            return be.INPUT_WATER_TANK;
                        }
                        if (localDir == context) {
                            return be.OUTPUT_WATER_TANK;
                        }
                    }
                    return null;
                }
        );
    }
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip,isPlayerSneaking);
        SpecificRealGazState inputState = INPUT_WATER_TANK.getState();
        CreateLang.builder().add(
                    Component.literal("input : ")
                            .append(
                        CROWNSLang.formatTemperature(inputState.temperature()).component()
                                .append( " | ")
                                .append(CROWNSLang.formatPressure(inputState.pressure()).component())
                                .append(" | ")
                                .append(
                                        Component.literal("x = " +(int) (inputState.vaporQuality() *100) + "%")
                                )))
                .forGoggles(tooltip, 1);
        SpecificRealGazState outputState = OUTPUT_WATER_TANK.getState();
        CreateLang.builder().add(
                Component.literal("output : ").append(
                        CROWNSLang.formatTemperature(outputState.temperature()).component()
                                .append( " | ")
                                .append(CROWNSLang.formatPressure(outputState.pressure()).component())
                                .append(" | ")
                                .append(
                                        Component.literal("x = " +(int) (outputState.vaporQuality() *100) + "%")
                                )))
                .forGoggles(tooltip, 1);
        return true;
    }
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag,registries, clientPacket);
        tag.putFloat("power",power);
        tag.put("input_water_tank", INPUT_WATER_TANK.writeToNBT(registries,new CompoundTag()));
        tag.put("output_water_tank", OUTPUT_WATER_TANK.writeToNBT(registries,new CompoundTag()));

    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        power = tag.getFloat("power");
        INPUT_WATER_TANK.readFromNBT(registries,(CompoundTag) tag.get("input_water_tank"));
        OUTPUT_WATER_TANK.readFromNBT(registries,(CompoundTag) tag.get("output_water_tank"));

        super.read(tag,registries, clientPacket);
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
            FluidStack water = INPUT_WATER_TANK.drain((int) Math.abs(speed), IFluidHandler.FluidAction.SIMULATE);
            if(!water.isEmpty()) {
                SpecificRealGazState outputState = WaterCubicEOSTransformationHelper.isentropicCompression(inputState, pressureRatio());
                power = (outputState.specificEnthalpy() - inputState.specificEnthalpy()) * water.getAmount() / Constants.whatSU;
                water.set(DataComponentsInit.REAL_GAZ_STATE, outputState);
                INPUT_WATER_TANK.drain(Math.min((int) Math.abs(speed),OUTPUT_WATER_TANK.fill(water, IFluidHandler.FluidAction.EXECUTE)), IFluidHandler.FluidAction.EXECUTE);
                if (hasNetwork() && speed != 0) {

                    KineticNetwork network = getOrCreateNetwork();
                    network.updateStressFor(this, calculateStressApplied());
                    network.updateStress();
                }
                notifyUpdate();
            }
        }
    }

}
