package com.rae.crowns.content.thermodynamics.conduction;

import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.content.thermodynamics.StateFluidTank;
import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.rae.crowns.init.misc.BlockInit;
import com.rae.formicapi.FormicApiLang;
import com.rae.formicapi.content.thermal_utilities.FullTableBased;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.rae.formicapi.content.thermal_utilities.FullTableBased.DEFAULT_STATE;


public class HeatExchangerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveTemperature {
    //really heavy -> to optimize and run less by second
    private static final int            SYNC_RATE   = 8;
    //for later maybe ? to make the code simpler to understand
    private final        StateFluidTank WATER_TANK  = new StateFluidTank(1000, (f) -> {
    }) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }
    };
    //transform the IHaveTemperature interface into a behavior
    // for now if T > 373°K P = 20 bar.
    public               float          C           = 3000 * 200;//specific thermal capacity J.K-1 it's a 3 ton metal assembly
    public               float          temperature = 300;
    protected            int            syncCooldown;
    protected            boolean        queuedSync;

    public HeatExchangerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityInit.HEAT_EXCHANGER.get(),
                (be, context) -> {
                    Direction localDir = be.getBlockState().getValue(DirectionalBlock.FACING);
                    if (context == localDir) {
                        return be.WATER_TANK;
                    }
                    if (context == localDir.getOpposite()) {
                        return be.WATER_TANK;
                    }
                    return null;
                }
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

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
            if (outState.is(BlockInit.HEAT_EXCHANGER.get())) {
                HeatExchangerBlockEntity be = (HeatExchangerBlockEntity) level.getBlockEntity(outPos);
                assert be != null;
                FluidTank handler = (FluidTank)
                        level.getCapability(Capabilities.FluidHandler.BLOCK, outPos, getBlockState().getValue(HeatExchangerBlock.FACING)
                        );
                if (handler == null) handler = new FluidTank(0);

                if (handler.getFluidAmount() < (float) WATER_TANK.getFluidAmount()) {//if input of following handler is smaller than ours
                    FluidStack stack = WATER_TANK.getFluid().copy();
                    stack.setAmount(WATER_TANK.getFluidAmount() - handler.getFluidAmount());
                    WATER_TANK.drain(handler.fill(stack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }

            //if not loaded we keep the same temperature.
            //internal conduction
            float  dt = 1 / 20f;
            double k  = getInternalConductivity() / getThermalCapacity() * CROWNSConfigs.SERVER.conduction.heatExchangerIterations.get();
            if (!WATER_TANK.isEmpty()) {//we don't heat it if empty
                int iteration = Math.max(1, (int) k * 1000 / WATER_TANK.getFluidAmount());
                for (int i = 0; i < iteration; i++) {
                    float power = getInternalConductivity() * (this.getTemperature() - WATER_TANK.getState().temperature()) * dt / iteration;
                    WATER_TANK.heat(power);
                    PhysicsWorldData data = PhysicsSaveManager.get((ServerLevel) level);
                    if (data != null && data.ticked(SectionPos.of(getBlockPos()).asLong())) {
                        this.addTemperature(-power / this.getThermalCapacity());
                    }
                }
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

    public float getInternalConductivity() {
        return CROWNSConfigs.SERVER.conduction.heatExchangerInternal.getF();
    }

    @Override
    public void lazyTick() {
        //What the fuck is going on here ?
        super.lazyTick();


        // the fact that it changes too often make it bugged ->
        // maybe if it's directly in  the fluidTransport behavior
        sendData();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("temperature", temperature);
        compound.put("water_tank", WATER_TANK.writeToNBT(registries, new CompoundTag()));

    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        super.writeSafe(compound, registries);
        compound.put("water_tank", WATER_TANK.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        temperature = compound.getFloat("temperature");
        CompoundTag fluidTag = (CompoundTag) compound.get("water_tank");
        WATER_TANK.readFromNBT(registries, fluidTag != null ? fluidTag : new CompoundTag());
        super.read(compound, registries, clientPacket);
    }

    @Override
    public float getThermalConductivity() {
        return CROWNSConfigs.SERVER.conduction.heatExchangerExternal.getF();
    }

    @Override
    public float getTemperature() {
        if (Float.isNaN(temperature)) {
            temperature = 300;
        }
        return temperature;
    }

    @Override
    public void addTemperature(float dT) {
        if (Float.isNaN(temperature)) {
            temperature = 300;
        }
        temperature = Math.max(temperature + dT, 0);
    }

    @Override
    public float getThermalCapacity() {
        return C;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder().add(Component.literal("exchanger "))
                .add(FormicApiLang.formatTemperature(temperature))
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);
        containedFluidTooltip(tooltip, isPlayerSneaking, WATER_TANK);

        return true;
    }

    // a Fluid Transport behavior that heat up water when going through. we need to modify the FluidNetwork to make it work.
    private static class HeatTransferBehaviour
            extends StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour {

        public HeatTransferBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return state.hasProperty(DirectionalBlock.FACING)
                    && state.getValue(DirectionalBlock.FACING).getAxis() == direction.getAxis();
        }

        @Override
        public FluidStack getProvidedOutwardFluid(Direction side) {
            FluidStack original = super.getProvidedOutwardFluid(side);

            return applyHeating(original);
        }

        private @NotNull FluidStack applyHeating(FluidStack original) {
            if (original.isEmpty())
                return original;

            if (!(blockEntity.getLevel() instanceof ServerLevel level))
                return original;

            if (!original.getFluid().isSame(Fluids.WATER))
                return original;

            FluidStack heated = original.copy();

            HeatExchangerBlockEntity exchanger = (HeatExchangerBlockEntity) blockEntity;

            float dt = 1 / 20f;

            double k = exchanger.getInternalConductivity()
                    / exchanger.getThermalCapacity()
                    * CROWNSConfigs.SERVER.conduction.heatExchangerIterations.get();

            int fluidAmount = heated.getAmount();
            int iteration   = Math.max(1, (int) (k * 1000 / fluidAmount));

            PhysicsWorldData data = PhysicsSaveManager.get(level);
            boolean canCoolBlock =
                    data != null && data.ticked(SectionPos.of(exchanger.getBlockPos()).asLong());

            for (int i = 0; i < iteration; i++) {
                // ENERGY, not temperature
                float power =
                        exchanger.getInternalConductivity()
                                * (exchanger.getTemperature()
                                - getFluidTemperature(heated)) // see helper below
                                * dt / iteration;

                // --- APPLY HEAT USING YOUR LOGIC ---
                heatFluidStack(heated, power);

                if (canCoolBlock) {
                    exchanger.addTemperature(-power / exchanger.getThermalCapacity());
                }
            }

            return heated;
        }

        private static float getFluidTemperature(FluidStack stack) {
            SpecificRealGazState state = stack.get(DataComponentsInit.REAL_GAZ_STATE);
            if (state == null)
                return DEFAULT_STATE.temperature();

            return state.temperature();
        }

        private static void heatFluidStack(FluidStack stack, float amount) {
            if (stack.getAmount() <= 0)
                return;

            SpecificRealGazState oldState = stack.get(DataComponentsInit.REAL_GAZ_STATE);
            if (oldState == null) oldState = DEFAULT_STATE;

            SpecificRealGazState newState =
                    FullTableBased.isobaricTransfer(oldState, amount / stack.getAmount());

            stack.set(DataComponentsInit.REAL_GAZ_STATE, newState);
        }

    }
}
