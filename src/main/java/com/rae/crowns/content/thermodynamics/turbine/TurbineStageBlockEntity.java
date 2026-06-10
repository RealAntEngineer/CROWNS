package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.sound.CrownsSoundScapes;
import com.rae.crowns.content.thermodynamics.ISteamPressureChange;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.NonnullDefault;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@NonnullDefault
public class TurbineStageBlockEntity extends GeneratingKineticBlockEntity implements ISteamPressureChange {
    public    int                initialTicks;
    //the turbine add itself to the SteamCurrent
    protected List<SteamCurrent> flows = List.of();
    LerpedFloat power = LerpedFloat.linear();
    int         index;

    public TurbineStageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
        initialTicks = 3;
    }

    @Override
    public float calculateAddedStressCapacity() {//it's the stress base not the real stress
        float capacity = (float) (getCombinedCapacity() * CROWNSConfigs.SERVER.kinetics.turbineCoefficient.get());
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    private float getCombinedCapacity() {
        if (level == null) return 0;

        return getGeneratedSpeed() == 0 ? power.getValue() : power.getValue() / getGeneratedSpeed();// capacity is
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("power", power.getValue());
        compound.putInt("index", index);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        power.setValue(compound.getFloat("power"));
        index = compound.getInt("index");
    }

    @Override
    public float getGeneratedSpeed() {
        //if flows is empty and power!=0 it means that the BE is being loaded, we need to trust only the power in that case
        //so there is no need to check for the flows.
        return power.getValue() == 0 ? 0 : Math.min(CROWNSConfigs.SERVER.kinetics.turbineSpeed.get(), AllConfigs.server().kinetics.maxRotationSpeed.get()); // * direction du flux
    }

    @Override
    public boolean isSource() {
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {
        super.tickAudio();
        if (Math.abs(speed) > 0) {
            CrownsSoundScapes.play(CrownsSoundScapes.AmbienceGroup.TURBINE, worldPosition, Mth.lerp(Math.abs(speed) / 256, 0.25f, 1));
        }

    }

    @Override
    protected boolean isNoisy() {
        return true;
    }

    @Override
    public float pressureRatio() {
        return 0.5f;
    }

    @Override
    public void tick() {
        super.tick();
        power.tickChaser();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        CreateLang.builder().add(Component.literal("stage number " + index))
                .style(ChatFormatting.DARK_RED)
                .forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void lazyTick() {
        assert level != null;
        if (level.isClientSide()) return;
        //update the List of currents
        AABB bound = new AABB(worldPosition);
        List<Direction.Axis> plane = Arrays.stream(Direction.Axis.values()).filter(
                (axis -> !axis.test(getBlockState().getValue(TurbineStageBlock.FACING)))).toList();
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.NEGATIVE, plane.get(0))
                .getNormal()));
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, plane.get(0))
                .getNormal()));
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.NEGATIVE, plane.get(1))
                .getNormal()));
        bound = bound.expandTowards(Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, plane.get(1))
                .getNormal()));

        flows = SteamFlowManager.getCurrentsInBounds((ServerLevel) level, bound);//level.getEntitiesOfClass(SteamCurrent.class, bound);
        AtomicReference<Float> newPower = new AtomicReference<>((float) 0);
        flows.forEach(f -> {
            SteamCurrent.SPR spr = f.getPowerForStage(this);
            newPower.updateAndGet(v -> v + spr.power());
            index = spr.stage();
        });
        power.chaseTimed(newPower.get(), 20);

        updateGeneratedRotation();
    }
}