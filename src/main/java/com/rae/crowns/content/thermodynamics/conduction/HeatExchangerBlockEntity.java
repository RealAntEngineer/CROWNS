package com.rae.crowns.content.thermodynamics.conduction;

import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.misc.BlockInit;

import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeatExchangerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveTemperature {
    //transform the IHaveTemperature interface into a behavior
    // for now if T > 373°K P = 20 bar.
    public float C = 3000*200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly
    public float temperature = 300;
    protected LazyOptional<IFluidHandler> fluidCapability;

    //for later maybe ? to make the code simpler to understand
    private final StateFluidTank WATER_TANK = new StateFluidTank(1000, (f)-> {}){
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };

    public HeatExchangerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

        fluidCapability = LazyOptional.of(() -> WATER_TANK);
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
                        be.getCapability(ForgeCapabilities.FLUID_HANDLER,getBlockState().getValue(HeatExchangerBlock.FACING)
                ).orElse(new FluidTank(0));
                if (handler.getFluidAmount()< (float) WATER_TANK.getFluidAmount()){//if input of following handler is smaller than ours
                    FluidStack stack =  WATER_TANK.getFluid().copy();
                    stack.setAmount(WATER_TANK.getFluidAmount() - handler.getFluidAmount());
                    WATER_TANK.drain(handler.fill(stack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }

            //internal conduction

            float deltaT = this.getTemperature() - WATER_TANK.getState().temperature();
            float k = this.getInternalConductivity();
            float dt = 0.05F; // time step duration in seconds

            float C_pipe = this.getThermalCapacity();

            // Calculate the heat transfer using exponential decay for stability
            float factor = 1 - (float)Math.exp(-k * dt / C_pipe);
            float heatTransfer = deltaT * factor;

            // Transfer heat to water
            WATER_TANK.heat(heatTransfer);
            temperature -= heatTransfer/C_pipe;
        }
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
    public void lazyTick() {
        //What the fuck is going on here ?
        super.lazyTick();
        //conductTemperature(getBlockPos(),level, 0.5f);


        // the fact that it changes too often make it bugged ->
        // maybe if it's directly in  the fluidTransport behaviour
        sendData();
    }

    @Override
    public float getThermalCapacity() {
        return C;
    }

    @Override
    public float getThermalConductivity() {
        return CROWNSConfigs.SERVER.conduction.heatExchangerExternal.getF();
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
        temperature+=dT;
    }
    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("temperature",temperature);
        tag.put("water_tank",WATER_TANK.writeToNBT(new CompoundTag()));

    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        temperature = tag.getFloat("temperature");
        WATER_TANK.readFromNBT((CompoundTag) tag.get("water_tank"));
        super.read(tag, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder().add(Component.literal("exchanger "))
                .add(FormicApiLang.formatTemperature(temperature))
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);

        return true;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            Direction localDir = this.getBlockState().getValue(DirectionalBlock.FACING);
            if (side == localDir){
                return this.fluidCapability.cast();
            }
            if (side ==  localDir.getOpposite()){
                return this.fluidCapability.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    // an entity that is responsible for searching an linking blocks that have fluid between them ?
    private static class FluidThermalConduction extends FluidManipulationBehaviour {

        public static final BehaviourType<FluidThermalConduction> TYPE = new BehaviourType<>();
        private Set<BlockPos> inContactBlocks;
        public FluidThermalConduction(SmartBlockEntity be) {
            super(be);
            inContactBlocks = new HashSet<>();
        }

        @Override
        public void tick() {
            super.tick();
        }

        @Override
        public BehaviourType<?> getType() {
            return TYPE;
        }
        public void findInContactBlocks(){
            reset();
        }
        public Set<BlockPos> getInContactBlocks() {
            return inContactBlocks;
        }
    }
}
