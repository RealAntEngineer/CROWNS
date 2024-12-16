package com.rae.crowns.content.thermodynamics.turbine;

import com.mojang.math.Vector3f;
import com.rae.crowns.api.flow.client.FlowParticleData;
import com.rae.crowns.api.flow.commun.FlowLine;
import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper;
import com.rae.crowns.init.BlockInit;
import com.rae.crowns.init.EntityDataSerializersInit;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.rae.crowns.api.Constants.whatSU;

public class SteamCurrent extends Entity{
	private static final EntityDataAccessor<AABB> SYNCED_BB_ACCESSOR = SynchedEntityData.defineId(SteamCurrent.class, EntityDataSerializersInit.BB_SERIALIZER);
	private static final EntityDataAccessor<HashMap<BlockPos, SpecificRealGazState>> SYNCED_STATE_MAP_ACCESSOR = SynchedEntityData.defineId(SteamCurrent.class, EntityDataSerializersInit.SM_SERIALIZER);
	private static final EntityDataAccessor<Boolean> SYNCED_RELOAD_SPLINE_ACCESSOR = SynchedEntityData.defineId(SteamCurrent.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Direction> SYNCED_DIRECTION_ACCESSOR = SynchedEntityData.defineId(SteamCurrent.class, EntityDataSerializers.DIRECTION);
	private static final EntityDataAccessor<BlockPos> SYNCED_INJECTOR_ACCESSOR = SynchedEntityData.defineId(SteamCurrent.class, EntityDataSerializers.BLOCK_POS);
	//private static final EntityDataAccessor<BlockPos> SYNCED_COLLECTOR_ACCESSOR = SynchedEntityData.defineId(SteamCurrent.class, EntityDataSerializers.BLOCK_POS);

	private SpecificRealGazState inputFluidState = null;
	public float maxDistance;
	ArrayList<BlockPos> stagesPos = new ArrayList<>();
	HashMap<BlockPos, Float> powerForStage = new HashMap<>();
	private FlowLine spline;

	private float flow;
	//TODO finish to assemble the bricks + test if it works
//Sync the AABB ?
	public SteamCurrent(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	public void initialize(BlockPos injectorPos,Direction direction, int maxDistance){
		this.entityData.set(SYNCED_INJECTOR_ACCESSOR,injectorPos);
		this.entityData.set(SYNCED_DIRECTION_ACCESSOR, direction);
		this.maxDistance = maxDistance;
		rebuild();
		/*if (collectorPos != null){
			return collectorPos;
		}
		else
			return null;*/
	}
	public float getPowerForStage(IPressureChange stage){
		calculateForStage(stage);
		return powerForStage.get(((BlockEntity)stage).getBlockPos());

	}
	public void rebuild() {

		BlockPos start = this.entityData.get(SYNCED_INJECTOR_ACCESSOR);
		maxDistance = explore(level, start, maxDistance, entityData.get(SYNCED_DIRECTION_ACCESSOR));
		if (maxDistance < 0.25f)
			setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
		else {
			float factor = maxDistance - 1;
			Vec3 scale = Vec3.atLowerCornerOf( entityData.get(SYNCED_DIRECTION_ACCESSOR).getNormal()).scale(factor);
			if (factor > 0) {
				AABB bound = new AABB(start.relative( entityData.get(SYNCED_DIRECTION_ACCESSOR))).expandTowards(scale);
				this.entityData.set(SYNCED_BB_ACCESSOR, bound);
				setBoundingBox(new AABB(start.relative( entityData.get(SYNCED_DIRECTION_ACCESSOR))).expandTowards(scale));
			}
			else {
				AABB bound =new AABB(start.relative( entityData.get(SYNCED_DIRECTION_ACCESSOR))).contract(scale.x, scale.y, scale.z).move(scale);
				this.entityData.set(SYNCED_BB_ACCESSOR, bound);
				setBoundingBox(new AABB(start.relative( entityData.get(SYNCED_DIRECTION_ACCESSOR))).contract(scale.x, scale.y, scale.z)
						.move(scale));
			}
		}
	}
	public void calculateForStage(IPressureChange addedStage){
		if (!stagesPos.contains(((BlockEntity)addedStage).getBlockPos())) {//do the list of blockPos or relative distance to take care of..
			stagesPos.add(((BlockEntity)addedStage).getBlockPos());
			stagesPos = new ArrayList<>(stagesPos.stream().filter(
					p -> level.getBlockEntity(p) instanceof IPressureChange).sorted(
					(s1, s2)-> ((this. entityData.get(SYNCED_DIRECTION_ACCESSOR).getAxisDirection() == Direction.AxisDirection.POSITIVE) ? 1:-1)*
							(Objects.requireNonNull(level.getBlockEntity(s1)).getBlockPos().get(this. entityData.get(SYNCED_DIRECTION_ACCESSOR).getAxis()) -
									(Objects.requireNonNull(level.getBlockEntity(s2))).getBlockPos().get(this. entityData.get(SYNCED_DIRECTION_ACCESSOR).getAxis()))).toList());//sort by distance
		}
		ArrayList<IPressureChange> stages = new ArrayList<>(
				stagesPos.stream().filter(
						p -> level.getBlockEntity(p) instanceof IPressureChange).map( p -> (IPressureChange)level.getBlockEntity(p)).toList());

		//rebuild the map
		powerForStage = new HashMap<>();
		HashMap<BlockPos, SpecificRealGazState>  stateMap = new HashMap<>();
		SpecificRealGazState previousState = getInputFluidState();
		stateMap.put(entityData.get(SYNCED_INJECTOR_ACCESSOR),previousState);
		//System.out.println("start water : "+previousState);
		//sorted to ensure correct thing
        int i = 0;
		for (IPressureChange stage:stages) {
			i++;
			SpecificRealGazState nextState = previousState;
			if (stage != null) {
				float pressureRatio = stage.pressureRatio();
				if (pressureRatio < 1) {
					nextState = WaterAsRealGazTransformationHelper.standardExpansion(previousState, 1 / pressureRatio);
				} else if (pressureRatio > 1) {
					nextState = WaterAsRealGazTransformationHelper.standardCompression(previousState, pressureRatio);
				}
				//need to ensure that it's empty before end
				//.get(this.direction.getAxis()
				powerForStage.put(((BlockEntity) stage).getBlockPos(), (previousState.specificEnthalpy() - nextState.specificEnthalpy()) * getFlow() / whatSU);
				//System.out.println("stage : "+i+" | "+nextState + "power : "+(previousState.specificEnthalpy() - nextState.specificEnthalpy()) * getFlow());
				previousState = nextState;
				stateMap.put(((BlockEntity) stage).getBlockPos(), nextState);
			}
		}
		this.entityData.set(SYNCED_STATE_MAP_ACCESSOR, stateMap);
		this.entityData.set(SYNCED_RELOAD_SPLINE_ACCESSOR, true);
	}

	public void setInputFluidState(SpecificRealGazState inputFluidState) {
		this.inputFluidState = inputFluidState;
	}

	public SpecificRealGazState getInputFluidState(){
        BlockEntity be = level.getBlockEntity(entityData.get(SYNCED_INJECTOR_ACCESSOR));
        if (be instanceof SteamInputBlockEntity){
            inputFluidState = ((SteamInputBlockEntity) be).getState();
        }
        if (inputFluidState==null){
			inputFluidState = WaterAsRealGazTransformationHelper.DEFAULT_STATE;
		}
        return inputFluidState;
	}


	public float explore(Level world, BlockPos start, float max, Direction facing) {
		//Vec3 directionVec = Vec3.atLowerCornerOf(facing.getNormal());
		// add 2 to the flow if the block is a turbine blade
		// Determine the distance of the air flow
		float distance = 0;
		for (int i = 1; i <= max; i++) {
            BlockPos currentPos = start.relative(facing, i);
            if (!world.isLoaded(currentPos))
                break;
            BlockState state = world.getBlockState(currentPos);
			if (!state.isAir()){
				//if (state.is(BlockInit.STEAM_COLLECTOR.get()))//collectorPos = currentPos;
				break;
			}
			distance++;
        }
		return distance;
	}
	public float getFlow(){
		BlockEntity be = level.getBlockEntity(entityData.get(SYNCED_INJECTOR_ACCESSOR));
		if (be instanceof SteamInputBlockEntity){
			flow = ((SteamInputBlockEntity) be).getFlow();
		}
		return flow;//Kg/s
	}
	@Override
	protected void defineSynchedData() {
		this.entityData.define(SYNCED_BB_ACCESSOR,new AABB(BlockPos.ZERO));
		this.entityData.define(SYNCED_STATE_MAP_ACCESSOR, new HashMap<>());
		this.entityData.define(SYNCED_RELOAD_SPLINE_ACCESSOR, false);
		this.entityData.define(SYNCED_DIRECTION_ACCESSOR, Direction.NORTH);
		this.entityData.define(SYNCED_INJECTOR_ACCESSOR, BlockPos.ZERO);
		//this.entityData.define(SYNCED_COLLECTOR_ACCESSOR, BlockPos.ZERO);

	}
	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
		this.entityData.set(SYNCED_BB_ACCESSOR,
				new AABB(
						BlockPos.of(nbt.getLong("startPos")),
						BlockPos.of(nbt.getLong("endPos")))
				);
		if (nbt.contains("BSpline"))
			this.spline = FlowLine.deserializeNBT(nbt.getCompound("BSpline"));

		if (nbt.contains("direction"))
			entityData.set(SYNCED_DIRECTION_ACCESSOR, Objects.requireNonNull(Direction.CODEC.byName(nbt.getString("direction"))));
		if (nbt.contains("injectorPos")){
			this.entityData.set(SYNCED_INJECTOR_ACCESSOR, BlockPos.of(nbt.getLong("injectorPos")));
		}
	}
	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
		AABB syncedBB = this.entityData.get(SYNCED_BB_ACCESSOR);
		if (spline!=null)
			nbt.put("BSpline", spline.serializeNBT());
		nbt.putLong("startPos", new BlockPos(syncedBB.minX,syncedBB.minY,syncedBB.minZ).asLong());
		nbt.putLong("endPos", new BlockPos(syncedBB.maxX,syncedBB.maxY,syncedBB.maxZ).asLong());
		nbt.putLong("injectorPos", this.entityData.get(SYNCED_INJECTOR_ACCESSOR).asLong());
        nbt.putString("direction", entityData.get(SYNCED_DIRECTION_ACCESSOR).getName());
    }

	@Override
	public void tick() {
		//System.out.println((level.isClientSide?"client":"server") +" : "+ getBoundingBox());
		if (level.isClientSide){
			setBoundingBox(this.entityData.get(SYNCED_BB_ACCESSOR));
			if (entityData.get(SYNCED_RELOAD_SPLINE_ACCESSOR)){
				try {
					entityData.set(SYNCED_RELOAD_SPLINE_ACCESSOR, false);
					HashMap<BlockPos, SpecificRealGazState> stateMap = this.entityData.get(SYNCED_STATE_MAP_ACCESSOR);
					if (stateMap.keySet().stream().filter(Objects::nonNull).toList().size() > 1) {
						Direction direction = this.entityData.get(SYNCED_DIRECTION_ACCESSOR);
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
													BlockPos injectorPos = entityData.get(SYNCED_INJECTOR_ACCESSOR);
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
														stateMap.get(p).vaporQuality() > 0?Color.WHITE.mixWith(new Color(0f,0f,1f,1f),1-stateMap.get(p).vaporQuality()):new Color(0f,0f,1f,1f)
										).toList()
								);
					} else {
						spline = null;
					}
				} catch (Exception e){
					spline = null;
				}
			}
			if (spline!=null && flow > 0){
				level.addParticle(new FlowParticleData(spline,0),  position().x, position().y, position().z, 0, 0, 0);
			}

		}
	}

	@Override
	public @NotNull Packet<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static EntityType.Builder<?> build(EntityType.Builder<SteamCurrent> currentEntityBuilder) {
        return currentEntityBuilder.sized(1, 1);
	}
}
