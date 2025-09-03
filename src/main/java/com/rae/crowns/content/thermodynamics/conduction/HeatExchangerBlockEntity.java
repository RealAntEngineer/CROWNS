package com.rae.crowns.content.thermodynamics.conduction;

import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.rae.formicapi.thermal_utilities.helper.WaterAsRealGaz;
import com.rae.crowns.CROWNSLang;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.crowns.init.misc.BlockEntityInit;

import com.rae.crowns.init.misc.BlockInit;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pipes.*;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.rae.formicapi.thermal_utilities.helper.WaterAsRealGaz.DEFAULT_STATE;

public class HeatExchangerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveTemperature {
    //transform the IHaveTemperature interface into a behavior
    // for now if T > 373°K P = 20 bar.
    public int C = 3000*200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly
    public float temperature = 300;

    //for later maybe ? to make the code simpler to understand
    private final StateFluidTank WATER_TANK = new StateFluidTank(1000, (f)-> {}){
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            setChanged();
        }
    };

    public HeatExchangerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        //behaviours.add(new FluidThermalConduction(this));
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level instanceof ServerLevel serverLevel) {
            TemperatureWorldData data = TemperatureManager.get(serverLevel);
            if (data != null) {
                data.putDynamic(getBlockPos(), this);
            }
        }
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
            //transmission logic

            BlockPos outPos = getBlockPos().relative(
                    getBlockState().getValue(HeatExchangerBlock.FACING));
            BlockState outState = level.getBlockState(outPos);
            if (outState.is(BlockInit.HEAT_EXCHANGER.get())){
                HeatExchangerBlockEntity be = (HeatExchangerBlockEntity) level.getBlockEntity(outPos);
                assert be != null;
                FluidTank handler = (FluidTank)
                        level.getCapability(Capabilities.FluidHandler.BLOCK,outPos,getBlockState().getValue(HeatExchangerBlock.FACING)
                );
                if (handler == null) handler = new FluidTank(0);

                if (handler.getFluidAmount()< (float) WATER_TANK.getFluidAmount()){//if input of following handler is smaller than ours
                    FluidStack stack =  WATER_TANK.getFluid().copy();
                    stack.setAmount(WATER_TANK.getFluidAmount() - handler.getFluidAmount());
                    WATER_TANK.drain(handler.fill(stack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }
            float dt = 1/20f;
            //conductTemperature(getBlockPos(),level, dt);
            double k = getInternalConductivity()/getThermalCapacity();
            heatWaterTank((int) k, dt);

            sendData();

        }
    }

    private void heatWaterTank(int k, float dt) {
        if (!WATER_TANK.isEmpty()) {//we don't heat it if empty
            int iteration = Math.max(1, k);
            for (int i = 0; i < iteration; i++) {
                float power = getInternalConductivity() * (this.getTemperature() - WATER_TANK.getState().temperature()) * dt / iteration;
                WATER_TANK.heat(power);
                this.addTemperature(
                        -power
                                / this.getThermalCapacity());
            }
        }
    }


    @Override
    public void lazyTick() {
        //What the fuck is going on here ?
        super.lazyTick();
    }

    @Override
    public int getThermalCapacity() {
        return C;
    }

    @Override
    public int getThermalConductivity() {
        return (int) CROWNSConfigs.SERVER.conduction.heatExchangerExternal.getF();
    }
    public float getInternalConductivity() {
        return CROWNSConfigs.SERVER.conduction.heatExchangerInternal.getF();
    }

    @Override
    public float getTemperature() {
        if (Float.isNaN(temperature)){
            temperature = 300;
        }
        return temperature;
    }

    @Override
    public void addTemperature(float dT) {
        if (Float.isNaN(temperature)){
            temperature = 300;
        }
        temperature=Math.max(temperature+dT,0);
    }
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag,registries, clientPacket);
        tag.putFloat("temperature",temperature);
        tag.put("water_tank",WATER_TANK.writeToNBT(registries,new CompoundTag()));

    }

    @Override
    protected void read(CompoundTag tag,HolderLookup.Provider registries, boolean clientPacket) {
        temperature = tag.getFloat("temperature");
        WATER_TANK.readFromNBT(registries,(CompoundTag) tag.get("water_tank"));
        super.read(tag, registries,clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder().add(Component.literal("exchanger "))
                .add(CROWNSLang.formatTemperature(temperature))
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);
        containedFluidTooltip(tooltip, isPlayerSneaking, WATER_TANK);

        return true;
    }


    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityInit.HEAT_EXCHANGER.get(),
                (be, context) -> {
                    Direction localDir = be.getBlockState().getValue(DirectionalBlock.FACING);
                    if (context == localDir){
                        return be.WATER_TANK;
                    }
                    if (context ==  localDir.getOpposite()){
                        return be.WATER_TANK;
                    }
                    return null;
                }
        );
    }
    // TODO find why it doesn't work.
    public static class FluidThermalConduction  extends StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour {

        public FluidThermalConduction(SmartBlockEntity be) {
            super(be);
        }
        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return state.getValue(DirectionalBlock.FACING).getAxis() == direction.getAxis();
        }

        @Override
        public @Nullable PipeConnection.Flow getFlow(Direction side) {
            PipeConnection.Flow flow = super.getFlow(side);
            assert flow != null;
            FluidStack stack = flow.fluid.copy();
            float dt = 1/20f;
            double k =((HeatExchangerBlockEntity) blockEntity).getInternalConductivity()/((HeatExchangerBlockEntity) blockEntity).getThermalCapacity();
            if (!stack.isEmpty()) {//we don't heat it if empty
                int iteration = (int) Math.max(1, k);
                for (int i = 0; i < iteration; i++) {
                    float power = ((HeatExchangerBlockEntity) blockEntity).getInternalConductivity() *
                            (((HeatExchangerBlockEntity) blockEntity).getTemperature() - getState(stack).temperature()) * dt / iteration;
                    heat(stack, power);
                    ((HeatExchangerBlockEntity) blockEntity).addTemperature(
                            -power
                                    / ((HeatExchangerBlockEntity) blockEntity).getThermalCapacity());
                }
            }
            ((HeatExchangerBlockEntity) blockEntity).WATER_TANK.setFluid(stack);
            flow.fluid = stack;
            return flow;
        }

        @Override
        public FluidStack getProvidedOutwardFluid(Direction side) {
            //we have a
            FluidStack stack = super.getProvidedOutwardFluid(side).copy();
            float dt = 1/20f;
            double k =((HeatExchangerBlockEntity) blockEntity).getInternalConductivity()/((HeatExchangerBlockEntity) blockEntity).getThermalCapacity();
            if (!stack.isEmpty()) {//we don't heat it if empty
                int iteration = (int) Math.max(1, k);
                for (int i = 0; i < iteration; i++) {
                    float power = ((HeatExchangerBlockEntity) blockEntity).getInternalConductivity() *
                            (((HeatExchangerBlockEntity) blockEntity).getTemperature() - getState(stack).temperature()) * dt / iteration;
                    heat(stack, power);
                    ((HeatExchangerBlockEntity) blockEntity).addTemperature(
                            -power
                                    / ((HeatExchangerBlockEntity) blockEntity).getThermalCapacity());
                }
            }
            ((HeatExchangerBlockEntity) blockEntity).WATER_TANK.setFluid(stack);
            return stack;
        }
        private SpecificRealGazState getState(FluidStack fluid){
            SpecificRealGazState state = fluid.get(DataComponentsInit.REAL_GAZ_STATE);

            if (state == null) {
                state = DEFAULT_STATE;
            }
            return state;
        }
        private void heat(FluidStack fluid, float amount){
            if (fluid.getAmount() > 0) {
                SpecificRealGazState state = WaterAsRealGaz.isobaricTransfer(getState(fluid), amount / fluid.getAmount());
                fluid.set(DataComponentsInit.REAL_GAZ_STATE, state);
            }
        }
    }
}
