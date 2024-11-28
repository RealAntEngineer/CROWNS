package com.rae.crowns.content.thermals;

import com.rae.crowns.api.nuclear.IHaveTemperature;
import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper;
import com.rae.crowns.init.BlockInit;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import static com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper.get_h;

public class HeatExchangerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveTemperature {
    //transform the IHaveTemperature interface into a behavior

    public float C = 3000*200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly
    public float temperature = 300;

    protected SmartFluidTankBehaviour inputTank;
    SpecificRealGazState inputState = new SpecificRealGazState(300f, 101300f, get_h(0,300,101300),0f);
    SpecificRealGazState outputState =  new SpecificRealGazState(300f, 101300f, get_h(0,300,101300),0f);
    float lastInputAmount = 0;
    protected SmartFluidTankBehaviour outputTank;
    float lastOutPutAmount;
    private boolean contentsChanged;
    protected LazyOptional<IFluidHandler> fluidCapability;

    float flow = 0; // (amount given + amount taken)/2 ? or min() ?
    public HeatExchangerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT,this,1,1000,false)
                .whenFluidUpdates(() -> contentsChanged = true)
                .forbidExtraction();
        behaviours.add(inputTank);
        outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT,this,1,1000,false)
                .whenFluidUpdates(() -> contentsChanged = true)
                .forbidInsertion();
        behaviours.add(outputTank);

        fluidCapability = LazyOptional.of(() -> {
            LazyOptional<? extends IFluidHandler> inputCap = inputTank.getCapability();
            LazyOptional<? extends IFluidHandler> outputCap = outputTank.getCapability();

            return new CombinedTankWrapper(outputCap.orElse(null), inputCap.orElse(null));
        });
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
        if (!level.isClientSide()) {
            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && queuedSync)
                    sendData();
            }
            inputTank.allowExtraction();
            inputTank.forbidInsertion();
            outputTank.allowInsertion();
            outputTank.forbidExtraction();

            CombinedTankWrapper fluidHandler = (CombinedTankWrapper) fluidCapability.orElse(new FluidTank(0));
            float flow = fluidHandler.getFluidInTank(1).getAmount() -lastInputAmount;

            int amountTransmitted = fluidHandler.fill(fluidHandler.getFluidInTank(1), IFluidHandler.FluidAction.EXECUTE);
            fluidHandler.drain(amountTransmitted, IFluidHandler.FluidAction.EXECUTE);
            lastInputAmount = fluidHandler.getFluidInTank(1).getAmount();
            outputTank.allowExtraction();
            outputTank.forbidInsertion();
            inputTank.allowInsertion();
            inputTank.forbidExtraction();
            if (flow > 0) {
                SpecificRealGazState partialOutput = inputState;
                int nbrOfStage = 10;
                for (int i = 0; i <nbrOfStage; i++) {
                    //make the calculus so it the real nbr or make it in stage ( like ten stage )
                    float power = getThermalConductivity() * (this.getTemperature() - partialOutput.temperature())/nbrOfStage / 20;
                    partialOutput = WaterAsRealGazTransformationHelper.isobaricTransfert(partialOutput, power / flow);
                    this.addTemperature(
                            -power
                                    / this.getThermalCapacity());
                }
                outputState = partialOutput;
            }
            //transmission logic
            BlockState positiveState = level.getBlockState(
                    getBlockPos().relative(
                            getBlockState().getValue(HeatExchangerBlock.AXIS),1));
            BlockState negativeState = level.getBlockState(
                    getBlockPos().relative(
                            getBlockState().getValue(HeatExchangerBlock.AXIS),-1));
            if (positiveState
                    .is(BlockInit.HEAT_EXCHANGER.get())){
                HeatExchangerBlockEntity be = (HeatExchangerBlockEntity) level.getBlockEntity(getBlockPos().relative(
                            getBlockState().getValue(HeatExchangerBlock.AXIS), 1));
                IFluidHandler handler = be.getCapability(ForgeCapabilities.FLUID_HANDLER,
                            Direction.fromAxisAndDirection(getBlockState().getValue(HeatExchangerBlock.AXIS),
                            Direction.AxisDirection.NEGATIVE)).orElse(new FluidTank(0));
                if (handler.getFluidInTank(0).getAmount() < (float) this.fluidCapability.orElse(new FluidTank(0))
                            .getFluidInTank(0).getAmount()){
                    this.fluidCapability.orElse(new FluidTank(0))
                                .drain(handler.fill(this.fluidCapability.orElse(new FluidTank(0))
                                .getFluidInTank(0), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    be.inputState = this.outputState;
                }
            }
            if (negativeState
                    .is(BlockInit.HEAT_EXCHANGER.get())){
                HeatExchangerBlockEntity be = (HeatExchangerBlockEntity) level.getBlockEntity(getBlockPos().relative(
                        getBlockState().getValue(HeatExchangerBlock.AXIS), -1));
                IFluidHandler handler = be.getCapability(ForgeCapabilities.FLUID_HANDLER,
                        Direction.fromAxisAndDirection(getBlockState().getValue(HeatExchangerBlock.AXIS),
                                Direction.AxisDirection.POSITIVE)).orElse(new FluidTank(0));
                if (handler.getFluidInTank(0).getAmount() < (float) this.fluidCapability.orElse(new FluidTank(0))
                        .getFluidInTank(0).getAmount()){
                    this.fluidCapability.orElse(new FluidTank(0))
                            .drain(handler.fill(this.fluidCapability.orElse(new FluidTank(0))
                                    .getFluidInTank(0), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    be.inputState = this.outputState;
                }
            }


        }
    }
    @Override
    public void lazyTick() {
        super.lazyTick();
        conductTemperature(getBlockPos(),level, 0.5f);

        sendData();
    }

    @Override
    public float getThermalCapacity() {
        return C;
    }

    @Override
    public float getThermalConductivity() {
        return 10000;
    }

    @Override
    public float getTemperature() {
        return temperature;
    }

    @Override
    public void addTemperature(float dT) {
        temperature+=dT;
    }
    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("temperature",temperature);
        CompoundTag positiveTankNBT = new CompoundTag();
        inputTank.write(positiveTankNBT,clientPacket);
        tag.put("positiveTank",positiveTankNBT);

    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        temperature = tag.getFloat("temperature");
        inputTank.read((CompoundTag) tag.get("positiveTank"),clientPacket);
        super.read(tag, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        Lang.builder().add(Component.literal("temperature : "+(int)temperature))
                .add(Component.literal("°K"))
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);
        IFluidHandler fluids = fluidCapability.orElse(new FluidTank(0));

        LangBuilder mb = Lang.translate("generic.unit.millibuckets");
        for (int i = 0; i < fluids.getTanks(); i++) {
            FluidStack fluidStack = fluids.getFluidInTank(i);
            if (fluidStack.isEmpty())
                continue;
            Lang.text(i+" ")
                    .add(Lang.fluidName(fluidStack)
                            .add(Lang.text(" "))
                            .style(ChatFormatting.GRAY)
                            .add(Lang.number(fluidStack.getAmount())
                                    .add(mb)
                                    .style(ChatFormatting.BLUE)))
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            Direction localDir = Direction.fromAxisAndDirection(this.getBlockState().getValue(HeatExchangerBlock.AXIS), Direction.AxisDirection.POSITIVE);
            if (side == localDir){
                return this.fluidCapability.cast();
            }
            if (side ==  localDir.getOpposite()){
                //return this.oxygenFluidOptional.cast();
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
