package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.thermodynamics.ISteamPressureChange;
import com.rae.crowns.init.misc.BlockInit;
import com.rae.flow.client.FlowParticleData;
import com.rae.flow.commun.FlowLine;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import com.rae.formicapi.thermal_utilities.helper.WaterAsRealGaz;
import com.rae.formicapi.thermal_utilities.helper.WaterTableBased;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.rae.crowns.Constants.whatSU;

public class SteamCurrent {

	/*public static Codec<SteamCurrent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					SpecificRealGazState.CODEC.optionalFieldOf("inputState",null).forGetter(s -> s.inputFluidState),
					SpecificRealGazState.CODEC.optionalFieldOf("outputState",null).forGetter(s -> s.outputFluidState),
					Codec.list(BlockPos.CODEC).optionalFieldOf("stagePos", new ArrayList<>()).forGetter(s -> s.stagesPos),
					BlockPos.CODEC.optionalFieldOf("collectorPos", null).forGetter(s -> s.collectorPos),
					BlockPos.CODEC.fieldOf("injectorPos").forGetter(s -> s.injectorPos),
					Direction.CODEC.fieldOf("direction").forGetter(s -> s.direction),
					Codec.FLOAT.fieldOf("maxDistance").forGetter(s -> s.maxDistance)

			).apply(instance, SteamCurrent::new));*/

    public float maxDistance;
    @NotNull ArrayList<BlockPos> stagesPos = new ArrayList<>();
    @Nullable BlockPos collectorPos = null;
    BlockPos injectorPos;
    @NotNull Map<BlockPos, Float> powerForStage = new ConcurrentHashMap<>();
    @NotNull HashMap<BlockPos, SpecificRealGazState> stateMap = new HashMap<>();
    private @Nullable SpecificRealGazState inputFluidState = null;
    private @Nullable SpecificRealGazState outputFluidState = null;
    private Direction direction;
    private @Nullable FlowLine spline;

	/*public SteamCurrent(SpecificRealGazState inputFluidState, SpecificRealGazState outputFluidState, float maxDistance,
						List<BlockPos> stagesPos, BlockPos collectorPos, BlockPos injectorPos, Direction direction,
						HashMap<BlockPos, Float> powerForStage, HashMap<BlockPos, SpecificRealGazState> stateMap,
						float flow, AABB bondingBox) {
		this.inputFluidState = inputFluidState;
		this.outputFluidState = outputFluidState;
		this.maxDistance = maxDistance;
		this.stagesPos = new ArrayList<>(stagesPos);
		this.collectorPos = collectorPos;
		this.injectorPos = injectorPos;
		this.direction = direction;
		this.powerForStage = powerForStage;
		this.stateMap = stateMap;
		this.flow = flow;
		this.bondingBox = bondingBox;
		this.reloadSpline = true;
	}*/

    private float flow;
    private AABB bondingBox;
    private boolean reloadSpline;

    //TODO finish to assemble the bricks + test if it works
//Sync the AABB ?
    public SteamCurrent(BlockPos sourcePosition, @NotNull Direction direction, float maxDistance) {
        this.injectorPos = sourcePosition;
        this.direction = direction;
        this.maxDistance = maxDistance;
        this.bondingBox = new AABB(injectorPos.relative(direction));
    }

    public SteamCurrent(BlockPos injectorPos, Direction direction, float maxDistance, AABB bondingBox, BlockPos collectorPos, FlowLine spline, boolean reloadSpline) {
        this.injectorPos = injectorPos;
        this.direction = direction;
        this.maxDistance = maxDistance;
        this.bondingBox = bondingBox;
        this.collectorPos = collectorPos;
        this.spline = spline;
        this.reloadSpline = reloadSpline;
    }

    public static @NotNull SteamCurrent fromNBT(@NotNull CompoundTag nbt) {
        AABB bondingBox =
                new AABB(
                        BlockPos.of(nbt.getLong("startPos")),
                        BlockPos.of(nbt.getLong("endPos")));
        BlockPos injectorPos = BlockPos.of(nbt.getLong("injectorPos"));
        float maxDistance = nbt.getFloat("maxDistance");
        boolean reloadSpline = nbt.getBoolean("reloadSpline");

        BlockPos collectorPos = null;
        FlowLine spline = null;
        Direction direction = null;
        if (nbt.contains("collectorPos")) {
            collectorPos = BlockPos.of(nbt.getLong("collectorPos"));
        }
        if (nbt.contains("BSpline"))
            spline = FlowLine.deserializeNBT(nbt.getCompound("BSpline"));

        if (nbt.contains("direction"))
            direction = Objects.requireNonNull(Direction.CODEC.byName(nbt.getString("direction")));

        return new SteamCurrent(injectorPos, direction, maxDistance, bondingBox, collectorPos, spline, reloadSpline);
    }

    protected @NotNull CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        AABB syncedBB = bondingBox;
        if (spline != null)
            nbt.put("BSpline", spline.serializeNBT());
        nbt.putLong("startPos", new BlockPos((int) syncedBB.minX, (int) syncedBB.minY, (int) syncedBB.minZ).asLong());
        nbt.putLong("endPos", new BlockPos((int) syncedBB.maxX, (int) syncedBB.maxY, (int) syncedBB.maxZ).asLong());
        nbt.putLong("injectorPos", injectorPos.asLong());
        if (collectorPos != null) nbt.putLong("collectorPos", this.collectorPos.asLong());
        nbt.putString("direction", direction.getName());
        nbt.putFloat("maxDistance", maxDistance);
        nbt.putBoolean("reloadSpline", reloadSpline);
        return nbt;
    }

    public float getPowerForStage(@NotNull ISteamPressureChange stage) {
        calculateForStage(stage, ((BlockEntity) stage).getLevel());
        return powerForStage.getOrDefault(((BlockEntity) stage).getBlockPos(), 0f);

    }

    public void rebuild(@NotNull Level level) {

        float distance = explore(level, injectorPos, maxDistance, direction);
        if (maxDistance < 0.25f)
            setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
        else {
            float factor = distance - 1;
            Vec3 scale = Vec3.atLowerCornerOf(direction.getNormal()).scale(factor);
            if (factor > 0) {
                //AABB bound = new AABB(injectorPos.relative(direction)).expandTowards(scale);
                setBoundingBox(new AABB(injectorPos.relative(direction)).expandTowards(scale));
            } else {
                //AABB bound =new AABB(injectorPos.relative( direction)).contract(scale.x, scale.y, scale.z).move(scale);
                setBoundingBox(new AABB(injectorPos.relative(direction)).contract(scale.x, scale.y, scale.z)
                        .move(scale));
            }
        }
        //put and end ?
    }

    private void setBoundingBox(@NotNull AABB aabb) {
        if (aabb.equals(bondingBox)) return;
        bondingBox = aabb;

    }

    public void calculateForStage(@NotNull ISteamPressureChange addedStage, @NotNull Level level) {
        final BlockPos addedPos = ((BlockEntity) addedStage).getBlockPos();

        // 1) Snapshot & mutate shared list under lock (short critical section)
        List<BlockPos> snapshot;
        synchronized (this) {
            // make a copy first to avoid modifying the list while someone else may iterate it
            List<BlockPos> local = new ArrayList<>(stagesPos);

            if (!local.contains(addedPos)) {
                local.add(addedPos);
            }
            // filter & sort on the local copy
            local = local.stream()
                    .filter(p -> level.getBlockEntity(p) instanceof ISteamPressureChange)
                    .sorted((s1, s2) -> ((direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) ? 1 : -1) *
                            (Objects.requireNonNull(level.getBlockEntity(s1)).getBlockPos().get(direction.getAxis()) -
                                    Objects.requireNonNull(level.getBlockEntity(s2)).getBlockPos().get(direction.getAxis())))
                    .toList();

            // publish the new list atomically
            stagesPos = new ArrayList<>(local);

            // snapshot the current list for processing
            snapshot = new ArrayList<>(stagesPos);
        } // lock released here

        // 2) Build stage instances from the snapshot (no lock held)
        List<ISteamPressureChange> stages = snapshot.stream()
                .map(level::getBlockEntity)
                .filter(e -> e instanceof ISteamPressureChange)
                .map(e -> (ISteamPressureChange) e)
                .toList();

        // 3) Rebuild the maps / compute physics (heavy work outside lock)
        powerForStage = new ConcurrentHashMap<>();
        HashMap<BlockPos, SpecificRealGazState> stateMap = new HashMap<>();
        SpecificRealGazState previousState = getInputFluidState(level);
        stateMap.put(injectorPos, previousState);

        SpecificRealGazState nextState = previousState;
        for (ISteamPressureChange stage : stages) {
            if (stage == null) continue;

            float pressureRatio = stage.pressureRatio();
            try {
                if (pressureRatio < 1f) {
                    nextState = WaterTableBased.isentropicExpansion(previousState, 1f / pressureRatio);
                } else if (pressureRatio > 1f) {
                    nextState = WaterTableBased.isentropicCompression(previousState, pressureRatio);
                }
            } catch (IllegalStateException error) {
                CROWNS.LOGGER.error("{} caused by trying to compress water from {} with a ratio of {}", error.getMessage(), previousState, pressureRatio);
                throw error;
            }
            float power = (previousState.specificEnthalpy() - nextState.specificEnthalpy()) * getFlow(level) * 20f / whatSU;
            powerForStage.put(((BlockEntity) stage).getBlockPos(), Float.isNaN(power) ? 0f : power);

            previousState = nextState;
            stateMap.put(((BlockEntity) stage).getBlockPos(), nextState);
        }

        this.outputFluidState = nextState;
        this.stateMap = stateMap;
        this.reloadSpline = true;
    }

    public void setInputFluidState(SpecificRealGazState inputFluidState) {
        this.inputFluidState = inputFluidState;

    }

    public @NotNull SpecificRealGazState getInputFluidState(@NotNull Level level) {
        BlockEntity be = level.getBlockEntity(injectorPos);
        if (be instanceof SteamInputBlockEntity) {
            inputFluidState = ((SteamInputBlockEntity) be).getState();
        }
        if (inputFluidState == null) {
            inputFluidState = WaterAsRealGaz.DEFAULT_STATE;
        }
        return inputFluidState;
    }

    public SpecificRealGazState getOutputFluidState() {
        return outputFluidState;
    }

    public float explore(@NotNull Level world, @NotNull BlockPos start, float max, @NotNull Direction facing) {
        //Vec3 directionVec = Vec3.atLowerCornerOf(facing.getNormal());
        // add 2 to the flow if the block is a turbine blade
        // Determine the distance of the air flow
        float distance = 0;
        for (int i = 1; i <= max; i++) {
            BlockPos currentPos = start.relative(facing, i);
            if (!world.isLoaded(currentPos))
                break;
            BlockState state = world.getBlockState(currentPos);
            if (!state.isAir()) {
                if (state.is(BlockInit.STEAM_COLLECTOR.get()) && state.getValue(DirectionalBlock.FACING) == getDirection().getOpposite())
                    collectorPos = currentPos;
                if (!state.is(BlockInit.TURBINE_STAGE_STRUCTURE.get())) {
                    break;
                }
            }
            distance++;
        }
        return distance;
    }

    public float getFlow(@NotNull Level level) {
        BlockEntity be = level.getBlockEntity(injectorPos);
        if (be instanceof SteamInputBlockEntity) {
            flow = ((SteamInputBlockEntity) be).getFlow();
        }
        return flow;//Kg/s
    }

    public void tick(@NotNull Level level) {
        //System.out.println((level.isClientSide?"client":"server") +" : "+ getBoundingBox());
        if (level.isClientSide) {
            //setBoundingBox(this.entityData.get(SYNCED_BB_ACCESSOR));
            if (this.reloadSpline) {
                try {
                    this.reloadSpline = false;
                    HashMap<BlockPos, SpecificRealGazState> stateMap = this.stateMap;
                    if (stateMap.keySet().stream().filter(Objects::nonNull).toList().size() > 1) {
                        Direction direction = this.direction;
                        List<BlockPos> sortedKeys = stateMap.keySet().stream().filter(p -> p != null && level.getBlockEntity(p) != null)
                                .sorted((s1, s2) -> ((direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) ? 1 : -1) *
                                        (Objects.requireNonNull(level.getBlockEntity(s1)).getBlockPos().get(direction.getAxis()) -
                                                (Objects.requireNonNull(level.getBlockEntity(s2))).getBlockPos().get(direction.getAxis())))

                                //.map(Vec3::atCenterOf)
                                .toList();
                        spline =
                                new FlowLine(sortedKeys.stream()
                                        .map(
                                                p -> {
                                                    BlockPos injectorPos = this.injectorPos;
                                                    return new BlockPos(direction.getStepX() == 0 ? injectorPos.getX() : p.getX(),
                                                            direction.getStepY() == 0 ? injectorPos.getY() : p.getY(),
                                                            direction.getStepZ() == 0 ? injectorPos.getZ() : p.getZ());

                                                }
                                        )
                                        .map(Vec3::atCenterOf).toList()
                                        ,
                                        List.of(0.1d),
                                        sortedKeys.stream().map(
                                                p ->
                                                        stateMap.get(p).vaporQuality() > 0 ? Color.WHITE.mixWith(new Color(0f, 0f, 1f, 1f), 1 - stateMap.get(p).vaporQuality()) : new Color(0f, 0f, 1f, 1f)
                                        ).toList()
                                );
                    } else {
                        spline = null;
                    }
                } catch (Exception e) {
                    spline = null;
                }
            }
            if (spline != null && flow > 0) {
                level.addParticle(new FlowParticleData(spline, 0), injectorPos.getX(), injectorPos.getY(), injectorPos.getZ(), 0, 0, 0);
            }
        } else {
            if (collectorPos != null) {
                BlockEntity be = level.getBlockEntity(collectorPos);
                if (be instanceof SteamCollectorBlockEntity steamCollector) {
                    try {
                        //cheating by getting the opposite side.
                        if (getDirection().getOpposite() == steamCollector.getBlockState().getValue(SteamCollectorBlock.FACING)) {
                            //TODO if the input is potion, this will transform it in water
                            CompoundTag nbt = new CompoundTag();
                            nbt.put("realGazState", getOutputFluidState().serialize());
                            steamCollector.getTank().fill(new FluidStack(Fluids.WATER, (int) getFlow(level), nbt), IFluidHandler.FluidAction.EXECUTE);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }


    public boolean isValid(@NotNull Level level) {
        if (level.getBlockEntity(injectorPos) instanceof SteamInputBlockEntity injector) {
            return !injector.isRemoved();
        }
        return false;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction facing) {
        direction = facing;
    }

    public boolean intersects(@NotNull AABB bound) {
        if (bondingBox == null) {
            return false;
        }
        return bondingBox.intersects(bound);
    }
}
