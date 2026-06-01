package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.CROWNS;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.ISteamPressureChange;
import com.rae.crowns.init.data.DataComponentsInit;
import com.rae.crowns.init.misc.BlockInit;
import com.rae.flow.client.FlowParticleData;
import com.rae.flow.commun.FlowLine;
import com.rae.formicapi.content.multiblock.MBStructureBlock;
import com.rae.formicapi.content.thermal_utilities.FullTableBased;
import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.rae.crowns.Constants.whatSU;
import static com.rae.formicapi.content.thermal_utilities.SpecificRealGasState.DEFAULT_STATE;

public class SteamCurrent {

    // configuration
    private final float                               maxDistance;
    @NotNull
    private final BlockPos                            injectorPos;
    boolean valid = true;
    // dynamic state
    @NotNull
    private       List<BlockPos>                      stagesPos     = new ArrayList<>();
    @Nullable
    private       BlockPos                            collectorPos;
    @NotNull
    private       Map<BlockPos, Float>                powerForStage = new ConcurrentHashMap<>();
    @NotNull
    private       Map<BlockPos, SpecificRealGasState> stateMap      = new HashMap<>();
    @Nullable
    private SpecificRealGasState inputFluidState;
    @Nullable
    private SpecificRealGasState outputFluidState;
    @NotNull
    private Direction direction;
    @Nullable
    private FlowLine  spline; // created on server and synced to client via NBT
    private float   flow;
    private AABB    boundingBox;
    private boolean reloadSpline;

    public SteamCurrent(@NotNull BlockPos injectorPos, @NotNull Direction direction, float maxDistance) {
        this.injectorPos = injectorPos;
        this.direction = direction;
        this.maxDistance = maxDistance;
        this.boundingBox = new AABB(injectorPos.relative(direction));
    }

    public SteamCurrent(
            @NotNull BlockPos injectorPos,
            @NotNull Direction direction,
            float maxDistance,
            @NotNull AABB boundingBox,
            @Nullable BlockPos collectorPos,
            @Nullable FlowLine spline,
            @NotNull List<BlockPos> stagesPos,
            boolean reloadSpline,
            @Nullable SpecificRealGasState inputFluidState,
            @Nullable SpecificRealGasState outputFluidState,
            @NotNull Map<BlockPos, Float> powerForStage,
            @NotNull Map<BlockPos, SpecificRealGasState> stateMap,
            float flow
    ) {
        this.injectorPos = injectorPos;
        this.direction = direction;
        this.maxDistance = maxDistance;
        this.boundingBox = boundingBox;
        this.collectorPos = collectorPos;
        this.spline = spline;
        this.reloadSpline = reloadSpline;
        this.stagesPos = stagesPos;
        this.inputFluidState = inputFluidState;
        this.outputFluidState = outputFluidState;
        this.powerForStage = powerForStage;
        this.stateMap = stateMap;
        this.flow = flow;
    }


    public static @NotNull SteamCurrent fromNBT(@NotNull CompoundTag nbt) {

        // Geometry + config
        BlockPos injectorPos = BlockPos.of(nbt.getLong("injectorPos"));
        AABB boundingBox = new AABB(
                Vec3.atLowerCornerOf(BlockPos.of(nbt.getLong("startPos"))),
                Vec3.atLowerCornerWithOffset(BlockPos.of(nbt.getLong("endPos")), 1, 1, 1)
        );

        float   maxDistance  = nbt.getFloat("maxDistance");
        boolean reloadSpline = nbt.getBoolean("reloadSpline");

        BlockPos collectorPos = nbt.contains("collectorPos") ? BlockPos.of(nbt.getLong("collectorPos")) : null;

        List<BlockPos> stagesPos =
                nbt.contains("stagesPos")
                        ? Arrays.stream(nbt.getLongArray("stagesPos")).mapToObj(BlockPos::of).toList()
                        : new ArrayList<>();

        Direction direction = nbt.contains("direction")
                ? Objects.requireNonNull(Direction.CODEC.byName(nbt.getString("direction")))
                : Direction.NORTH;

        FlowLine spline = nbt.contains("BSpline")
                ? FlowLine.deserializeNBT(nbt.getCompound("BSpline"))
                : null;

        float flow = nbt.getFloat("flow");

        // Input/output states
        SpecificRealGasState inputFluidState =
                nbt.contains("inputState") ? new SpecificRealGasState(nbt.getCompound("inputState")) : null;

        SpecificRealGasState outputFluidState =
                nbt.contains("outputState") ? new SpecificRealGasState(nbt.getCompound("outputState")) : null;

        // powerForStage map
        Map<BlockPos, Float> powerForStage = new HashMap<>();
        if (nbt.contains("powerForStage", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("powerForStage", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                CompoundTag tag = (CompoundTag) t;
                powerForStage.put(BlockPos.of(tag.getLong("pos")), tag.getFloat("power"));
            }
        }

        // stateMap
        Map<BlockPos, SpecificRealGasState> stateMap = new HashMap<>();
        if (nbt.contains("stateMap", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("stateMap", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                CompoundTag tag = (CompoundTag) t;
                stateMap.put(
                        BlockPos.of(tag.getLong("pos")),
                        new SpecificRealGasState(tag.getCompound("state"))
                );
            }
        }

        return new SteamCurrent(
                injectorPos,
                direction,
                maxDistance,
                boundingBox,
                collectorPos,
                spline,
                stagesPos,
                reloadSpline,
                inputFluidState,
                outputFluidState,
                powerForStage,
                stateMap,
                flow
        );
    }

    protected @NotNull CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();

        // Base geometry + config
        nbt.putLong("injectorPos", injectorPos.asLong());
        nbt.putLong("startPos", new BlockPos((int) boundingBox.minX, (int) boundingBox.minY, (int) boundingBox.minZ).asLong());
        nbt.putLong("endPos", new BlockPos((int) boundingBox.maxX, (int) boundingBox.maxY, (int) boundingBox.maxZ).asLong());
        nbt.putString("direction", direction.getName());
        nbt.putFloat("maxDistance", maxDistance);
        nbt.putBoolean("reloadSpline", reloadSpline);
        nbt.putLongArray("stagesPos", stagesPos.stream().mapToLong(BlockPos::asLong).toArray());

        if (collectorPos != null)
            nbt.putLong("collectorPos", collectorPos.asLong());

        // Flow
        nbt.putFloat("flow", flow);

        // Input/output states
        if (inputFluidState != null)
            nbt.put("inputState", inputFluidState.serialize());
        if (outputFluidState != null)
            nbt.put("outputState", outputFluidState.serialize());

        // Spline
        if (spline != null)
            nbt.put("BSpline", spline.serializeNBT());

        // powerForStage map
        ListTag powerList = new ListTag();
        for (var e : powerForStage.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", e.getKey().asLong());
            t.putFloat("power", e.getValue());
            powerList.add(t);
        }
        nbt.put("powerForStage", powerList);

        // stateMap
        ListTag stateList = new ListTag();
        for (var e : stateMap.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", e.getKey().asLong());
            t.put("state", e.getValue().serialize());
            stateList.add(t);
        }
        nbt.put("stateMap", stateList);

        return nbt;
    }


    /**
     * Returns the (stageIndex, power) record for a given pressure-change stage.
     * This no longer triggers recomputation — recomputeStages is run on the server tick and spline is built on the server.
     */
    public @NotNull SPR getPowerForStage(@NotNull ISteamPressureChange stage) {
        if (!(stage instanceof BlockEntity be)) return new SPR(-1, 0f);
        BlockPos pos   = be.getBlockPos();
        int      idx   = stagesPos.indexOf(pos);
        float    power = powerForStage.getOrDefault(pos, 0f);
        return new SPR(idx, power);
    }

    public void setInputFluidState(@NotNull SpecificRealGasState inputFluidState) {
        this.inputFluidState = inputFluidState;
    }

    /**
     * Main tick. Server: recompute states & build spline. Client: render particles from server-synced spline.
     */
    public void tick(@NotNull Level level) {
        if (!level.isClientSide) {
            // SERVER SIDE: recompute physics and build spline
            recomputeStages(level);

            if (reloadSpline) {
                try {
                    rebuildSplineServer(level);
                } catch (Exception e) {
                    spline = null;
                }
                reloadSpline = false;
            }

            // transfer to collector if present
            if (collectorPos != null) {
                transferToCollector(level);
            }

        } else {
            // CLIENT SIDE: only rendering
            if (spline != null && flow > 0) {
                level.addParticle(
                        new FlowParticleData(spline, 0),
                        injectorPos.getX(), injectorPos.getY(), injectorPos.getZ(),
                        0, 0, 0);
            }
        }
    }

    /**
     * Recompute stages' states and powers. Server-only heavy work.
     */
    public void recomputeStages(@NotNull Level level) {
        // build stages list from current stagesPos
        List<ISteamPressureChange> stages = new ArrayList<>();
        for (BlockPos p : stagesPos) {
            BlockEntity e = level.getChunkAt(p).getBlockEntity(p, LevelChunk.EntityCreationType.CHECK);
            if (e instanceof ISteamPressureChange spc) stages.add(spc);
        }

        // recompute physics
        powerForStage = new ConcurrentHashMap<>();
        Map<BlockPos, SpecificRealGasState> newStateMap = new HashMap<>();

        SpecificRealGasState previousState = getInputFluidState(level);
        newStateMap.put(injectorPos, previousState);

        SpecificRealGasState nextState   = previousState;
        float                yield       = CROWNSConfigs.SERVER.kinetics.turbineIsentropicYield.getF();
        float                minPressure = CROWNSConfigs.SERVER.kinetics.turbineLowPressureBound.getF();



        for (ISteamPressureChange stage : stages) {
            if (!(stage instanceof BlockEntity stageBe)) continue;

            float pressureRatio = stage.pressureRatio();

            float targetPressure = previousState.pressure() * pressureRatio;

            if (targetPressure < minPressure) {
                // Adjust pressure ratio to respect the minimum pressure
                pressureRatio = minPressure / previousState.pressure();
                if (pressureRatio < 1f) {
                    pressureRatio = 1f; // No expansion possible
                }
            }

            try {
                if (pressureRatio < 1f) {
                    nextState = FullTableBased.isentropicExpansion(previousState, 1f / pressureRatio);
                } else if (pressureRatio > 1f) {
                    nextState = FullTableBased.isentropicCompression(previousState, pressureRatio);
                }
            } catch (IllegalStateException error) {
                CROWNS.LOGGER.error("{} caused by trying to expand water from {} with a ratio of {}",
                        error.getMessage(), previousState, pressureRatio);
                throw error;
            }

            float power = (previousState.specificEnthalpy() - nextState.specificEnthalpy()) * getFlow(level) * 20f / whatSU * yield;
            powerForStage.put(stageBe.getBlockPos(), Float.isNaN(power) ? 0f : power);

            previousState = nextState;
            newStateMap.put(stageBe.getBlockPos(), nextState);
        }

        this.outputFluidState = nextState;
        this.stateMap = newStateMap;
        this.reloadSpline = true; // mark server to rebuild spline and sync it
    }

    /**
     * Build the FlowLine on the server from the current stateMap and stagesPos.
     */
    private void rebuildSplineServer(@NotNull Level level) {
        if (stateMap.size() <= 1) {
            spline = null;
            return;
        }

        Direction      dir        = this.direction;
        List<BlockPos> sortedKeys = new ArrayList<>();
        for (BlockPos p : stateMap.keySet()) {
            if (p != null && level.getChunkAt(p).getBlockEntity(p) != null) sortedKeys.add(p);
        }

        final int sign = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        sortedKeys.sort((s1, s2) -> sign * Integer.compare(s1.get(dir.getAxis()), s2.get(dir.getAxis())));

        List<Vec3>  points = new ArrayList<>(sortedKeys.size());
        List<Color> colors = new ArrayList<>(sortedKeys.size());

        for (BlockPos p : sortedKeys) {
            BlockPos ip = this.injectorPos;
            BlockPos aligned = new BlockPos(
                    dir.getStepX() == 0 ? ip.getX() : p.getX(),
                    dir.getStepY() == 0 ? ip.getY() : p.getY(),
                    dir.getStepZ() == 0 ? ip.getZ() : p.getZ()
            );
            points.add(Vec3.atCenterOf(aligned));

            SpecificRealGasState s = stateMap.get(p);
            Color color = (s != null && s.vaporQuality() > 0)
                    ? Color.WHITE.mixWith(new Color(0f, 0f, 1f, 1f), 1 - s.vaporQuality())
                    : new Color(0f, 0f, 1f, 1f);
            colors.add(color);
        }

        this.spline = new FlowLine(points, List.of(0.1d), colors);
    }

    private void transferToCollector(@NotNull Level level) {
        assert collectorPos != null;
        BlockEntity be = level.getChunkAt(collectorPos).getBlockEntity(collectorPos);
        if (be instanceof SteamCollectorBlockEntity steamCollector) {
            try {
                if (getDirection().getOpposite() == steamCollector.getBlockState().getValue(SteamCollectorBlock.FACING)) {
                    //TODO if the input is potion, this will transform it in water
                    FluidStack water = new FluidStack(Fluids.WATER, (int) getFlow(level));
                    water.set(DataComponentsInit.REAL_GAS_STATE, getOutputFluidState());
                    steamCollector.getTank().fill(water, IFluidHandler.FluidAction.EXECUTE);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public @NotNull SpecificRealGasState getInputFluidState(@NotNull Level level) {
        BlockEntity be = level.getChunkAt(injectorPos).getBlockEntity(injectorPos);
        if (be instanceof SteamInputBlockEntity) {
            inputFluidState = ((SteamInputBlockEntity) be).getState();
            valid = true;
        } else {
            valid = false;
        }
        if (inputFluidState == null) inputFluidState = DEFAULT_STATE;
        return inputFluidState;
    }

    public float getFlow(@NotNull Level level) {
        BlockEntity be = level.getChunkAt(injectorPos).getBlockEntity(injectorPos, LevelChunk.EntityCreationType.CHECK);
        if (be instanceof SteamInputBlockEntity) {
            flow = ((SteamInputBlockEntity) be).getFlow();
        }
        return flow; // Kg/s
    }

    public @NotNull Direction getDirection() {
        return direction;
    }

    public @NotNull SpecificRealGasState getOutputFluidState() {
        if (outputFluidState == null) outputFluidState = DEFAULT_STATE;
        return outputFluidState;
    }

    public void setDirection(@NotNull Direction facing) {
        direction = facing;
    }

    // Rebuild stages and spline (server-side)
    public void rebuild(@NotNull Level level) {
        // Original rebuild logic – computes stage positions and bounding box
        float distance = explore(level, injectorPos, maxDistance, direction);


        if (maxDistance < 0.25f) {
            setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
        } else {
            float factor = distance - 1;
            Vec3  scale  = Vec3.atLowerCornerOf(direction.getNormal()).scale(factor);


            if (factor > 0) {
                setBoundingBox(new AABB(injectorPos.relative(direction)).expandTowards(scale));
            } else {
                setBoundingBox(new AABB(injectorPos.relative(direction))
                        .contract(scale.x, scale.y, scale.z)
                        .move(scale));
            }
        }
    }

    /**
     * Explore along facing direction and collect valid controller positions for turbine stages.
     * Returns the distance traveled (number of non-air steps encountered).
     */
    public float explore(@NotNull Level world, @NotNull BlockPos start, float max, @NotNull Direction facing) {
        float          distance    = 0f;
        List<BlockPos> foundStages = new ArrayList<>();

        for (int i = 1; i <= max; i++) {
            BlockPos currentPos = start.relative(facing, i);
            if (!world.isLoaded(currentPos)) break;
            BlockState state = world.getBlockState(currentPos);

            if (!state.isAir()) {
                if (state.is(BlockInit.STEAM_COLLECTOR.get()) &&
                        state.getValue(DirectionalBlock.FACING) == getDirection().getOpposite()) {
                    collectorPos = currentPos;
                }

                if (!state.is(BlockInit.TURBINE_STAGE_STRUCTURE.get())) {
                    break;
                } else {
                    BlockPos    controller       = MBStructureBlock.getMaster(world, currentPos);
                    BlockEntity controllerEntity = world.getChunkAt(controller).getBlockEntity(controller);
                    if (controllerEntity instanceof TurbineStageBlockEntity) {
                        Direction controllerFacing = world.getBlockState(controller).getValue(DirectionalBlock.FACING);
                        if (facing.getAxis() == controllerFacing.getAxis()) {
                            foundStages.add(controller);
                        }
                    }
                }
            }
            distance++;
        }

        this.stagesPos = new ArrayList<>(foundStages);
        return distance;
    }

    public boolean isValid(@NotNull Level level) {
        return level.isClientSide || valid;
    }

    public boolean intersects(@NotNull AABB bound) {
        return boundingBox != null && boundingBox.intersects(bound);
    }

    public AABB getBoundingBox() {
        return boundingBox;
    }

    private void setBoundingBox(@NotNull AABB aabb) {
        boundingBox = aabb;
    }

    public record SPR(int stage, float power) {
    }
}
