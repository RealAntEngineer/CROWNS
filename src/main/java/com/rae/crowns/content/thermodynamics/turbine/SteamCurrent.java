package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import com.rae.crowns.api.transformations.WaterAsRealGazTransformationHelper;
import com.rae.crowns.init.BlockInit;
import com.rae.crowns.init.EntityDataSerializersInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
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
import java.util.Objects;

public class SteamCurrent extends Entity{
	private static final EntityDataAccessor<AABB> SYNCED_BB_ACCESSOR = SynchedEntityData.defineId(SteamCurrent.class, EntityDataSerializersInit.BB_SERIALIZER);
	private SpecificRealGazState inputFluidState = null;
	private BlockPos injectorPos = null;
	private BlockPos collectorPos = null;
	public Direction direction;
	public float maxDistance;
	ArrayList<BlockPos> stagesPos = new ArrayList<>();
	HashMap<BlockPos, Float> powerForStage = new HashMap<>();
	private float flow;
	//TODO finish to assemble the bricks + test if it works
//Sync the AABB ?
	public SteamCurrent(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	public BlockPos initialize(BlockPos injectorPos,Direction direction, int maxDistance){
		this.injectorPos = injectorPos;
		this.direction = direction;
		this.maxDistance = maxDistance;
		rebuild();
		if (collectorPos != null){
			return collectorPos;
		}
		else
			return null;
	}
	public float getPowerForStage(IPressureChange stage){
		calculateForStage(stage);
		return powerForStage.get(((BlockEntity)stage).getBlockPos());

	}
	public void rebuild() {

		BlockPos start = injectorPos;
		maxDistance = explore(level, start, maxDistance, direction);
		if (maxDistance < 0.25f)
			setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
		else {
			float factor = maxDistance - 1;
			Vec3 scale = Vec3.atLowerCornerOf(direction.getNormal()).scale(factor);
			if (factor > 0) {
				AABB bound = new AABB(start.relative(direction)).expandTowards(scale);
				this.entityData.set(SYNCED_BB_ACCESSOR, bound);
				setBoundingBox(new AABB(start.relative(direction)).expandTowards(scale));
			}
			else {
				AABB bound =new AABB(start.relative(direction)).contract(scale.x, scale.y, scale.z).move(scale);
				this.entityData.set(SYNCED_BB_ACCESSOR, bound);
				setBoundingBox(new AABB(start.relative(direction)).contract(scale.x, scale.y, scale.z)
						.move(scale));
			}
		}
	}
	public void calculateForStage(IPressureChange addedStage){
		if (!stagesPos.contains(addedStage.getBlockPos())) {//do the list of blockPos or relative distance to take care of..
			stagesPos.add(addedStage.getBlockPos());
			stagesPos = new ArrayList<>(stagesPos.stream().filter(
					p -> level.getBlockEntity(p) instanceof IPressureChange).sorted(
					(s1, s2)-> ((this.direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) ? -1:1)*
							(Objects.requireNonNull(level.getBlockEntity(s1)).getBlockPos().get(this.direction.getAxis()) -
									(Objects.requireNonNull(level.getBlockEntity(s2))).getBlockPos().get(this.direction.getAxis()))).toList());//sort by distance
		}
		ArrayList<IPressureChange> stages = new ArrayList<>(
				stagesPos.stream().map( p -> (IPressureChange)level.getBlockEntity(p)).toList());

		//rebuild the map
		powerForStage = new HashMap<>();
		SpecificRealGazState previousState = getInputFluidState();
		//System.out.println("start water : "+previousState);
		//sorted to ensure correct thing
		if (direction == null){
			direction = Direction.NORTH;
		}
		int i = 0;
		for (IPressureChange stage:stages){
			i++;
			SpecificRealGazState nextState = previousState;
			float pressureRatio = stage.pressureRatio();
			if (pressureRatio < 1) {
				nextState = WaterAsRealGazTransformationHelper.standardExpansion(previousState, 1/pressureRatio);
			} else if (pressureRatio > 1) {
				nextState = WaterAsRealGazTransformationHelper.standardCompression(previousState, pressureRatio);
			}
			//need to ensure that it's empty before end
			//.get(this.direction.getAxis()
			powerForStage.put(((BlockEntity) stage).getBlockPos(), (previousState.specificEnthalpy() - nextState.specificEnthalpy()) * getFlow());
			//System.out.println("stage : "+i+" | "+nextState + "power : "+(previousState.specificEnthalpy() - nextState.specificEnthalpy()) * getFlow());
			previousState = nextState;
		}

	}

	public void setInputFluidState(SpecificRealGazState inputFluidState) {
		this.inputFluidState = inputFluidState;
	}

	public SpecificRealGazState getInputFluidState(){
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
				if (state.is(BlockInit.STEAM_COLLECTOR.get()))
					collectorPos = currentPos;
				break;
			}
			distance++;
        }
		return distance;
	}

	public void setFlow(float flow) {
		this.flow = flow;
	}

	public float getFlow(){
		return flow;//Kg/s
	}
	@Override
	protected void defineSynchedData() {
		this.entityData.define(SYNCED_BB_ACCESSOR,new AABB(BlockPos.ZERO));
	}
	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
		this.entityData.set(SYNCED_BB_ACCESSOR,
				new AABB(
						BlockPos.of(nbt.getLong("startPos")),
						BlockPos.of(nbt.getLong("endPOs")))
				);
		if (nbt.contains("direction"))
			direction = Direction.CODEC.byName(nbt.getString("direction"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
		AABB syncedBB = this.entityData.get(SYNCED_BB_ACCESSOR);
		nbt.putLong("startPos", new BlockPos(syncedBB.minX,syncedBB.minY,syncedBB.minZ).asLong());
		nbt.putLong("endPos", new BlockPos(syncedBB.maxX,syncedBB.maxY,syncedBB.maxZ).asLong());
		if (direction!=null) {
			nbt.putString("direction", direction.getName());
		}
	}

	@Override
	public void tick() {
		//System.out.println((level.isClientSide?"client":"server") +" : "+ getBoundingBox());
		if (level.isClientSide){
			setBoundingBox(this.entityData.get(SYNCED_BB_ACCESSOR));
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
