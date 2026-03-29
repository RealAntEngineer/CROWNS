package com.rae.crowns.mixin;

import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.formicapi.thermal_utilities.FullTableBased;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.simibubi.create.content.fluids.FluidReactions;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(FluidTransportBehaviour.class)
public abstract class FluidTransportBehaviourMixin extends BlockEntityBehaviour {


    @Shadow(remap = false)
    public Map<Direction, PipeConnection> interfaces;

    @Shadow(remap = false)
    public FluidTransportBehaviour.UpdatePhase phase;

    public FluidTransportBehaviourMixin(SmartBlockEntity be) {
        super(be);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    public void replaceTick(CallbackInfo ci) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        super.tick();
        Level world = getWorld();
        BlockPos pos = getPos();
        boolean onServer = !world.isClientSide || blockEntity.isVirtual();

        if (interfaces == null)
            return;
        Collection<PipeConnection> connections = interfaces.values();

        // Do not provide a lone pipe connection with its own flow input
        PipeConnection singleSource = null;

//		if (onClient) {
//			connections.forEach(connection -> {
//				connection.visualizeFlow(pos);
//				connection.visualizePressure(pos);
//			});
//		}

        if (phase == FluidTransportBehaviour.UpdatePhase.WAIT_FOR_PUMPS) {
            phase = FluidTransportBehaviour.UpdatePhase.FLIP_FLOWS;
            return;
        }

        if (onServer) {
            boolean sendUpdate = false;
            for (PipeConnection connection : connections) {
                sendUpdate |= connection.flipFlowsIfPressureReversed();
                //dirty hack to make it work for 6.0.4
                try {
                    Method m = PipeConnection.class.getMethod("manageSource", Level.class, BlockPos.class, BlockEntity.class);
                    m.invoke(connection, world, pos, blockEntity);
                } catch (NoSuchMethodException e) {
                    Method m = PipeConnection.class.getMethod("manageSource", Level.class, BlockPos.class);
                    m.invoke(connection, world, pos);
                }//if this doesn't work we crash
            }
            if (sendUpdate)
                blockEntity.notifyUpdate();
        }

        if (phase == FluidTransportBehaviour.UpdatePhase.FLIP_FLOWS) {
            phase = FluidTransportBehaviour.UpdatePhase.IDLE;
            return;
        }

        if (onServer) {
            FluidStack availableFlow = FluidStack.EMPTY;
            FluidStack collidingFlow = FluidStack.EMPTY;

            for (PipeConnection connection : connections) {
                FluidStack fluidInFlow = connection.getProvidedFluid();
                if (fluidInFlow.isEmpty())
                    continue;
                if (availableFlow.isEmpty()) {
                    singleSource = connection;
                    availableFlow = fluidInFlow;
                    continue;
                }
                if (availableFlow.isFluidEqual(fluidInFlow)) {
                    // maybe me change this condition so no need
                    // to modify the equal call ?

                    //modified part
                    singleSource = null;
                    PatchedDataComponentMap inFlowTag = fluidInFlow.getComponents();
                    SpecificRealGazState inFlowState = inFlowTag.getOrDefault(DataComponentsInit.REAL_GAZ_STATE, FullTableBased.DEFAULT_STATE);
                    PatchedDataComponentMap availableTag = availableFlow.getComponents();
                    SpecificRealGazState availableState = availableTag.getOrDefault(DataComponentsInit.REAL_GAZ_STATE, FullTableBased.DEFAULT_STATE);
                    SpecificRealGazState mixedState = FullTableBased.mix(availableState, availableFlow.getAmount(),
                            inFlowState, fluidInFlow.getAmount());

                    availableFlow = fluidInFlow;
                    availableTag.set(DataComponentsInit.REAL_GAZ_STATE, mixedState);
                    availableFlow.applyComponents(availableTag);

                    continue;
                    //end of modified part
                }
                collidingFlow = fluidInFlow;
                break;
            }

            if (!collidingFlow.isEmpty()) {
                FluidReactions.handlePipeFlowCollision(world, pos, availableFlow, collidingFlow);
                return;
            }

            boolean sendUpdate = false;
            for (PipeConnection connection : connections) {
                FluidStack internalFluid = singleSource != connection ? availableFlow : FluidStack.EMPTY;
                Predicate<FluidStack> extractionPredicate =
                        extracted -> canPullFluidFrom(extracted, blockEntity.getBlockState(), connection.side);
                sendUpdate |= connection.manageFlows(world, pos, internalFluid, extractionPredicate);
            }

            if (sendUpdate)
                blockEntity.notifyUpdate();
        }

        for (PipeConnection connection : connections)
            connection.tickFlowProgress(world, pos);
        ci.cancel();
    }

    @Shadow(remap = false)
    public abstract boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction);
}
