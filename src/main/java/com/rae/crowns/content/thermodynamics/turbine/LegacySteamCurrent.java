package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.api.thermal_utilities.ISteamCurrentSource;
import com.rae.crowns.init.TagsInit;
import com.rae.crowns.content.legacy.TurbineContraption;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LegacySteamCurrent {
	//Inspired from AirCurrent
	// put a turbine chamber max size -> for now when building all of
	// them construct a list of AABB for the size of the room -> expansion factor ?

	//TODO transform that to work with the rest :
	// will have an entry point and an ouput
	// a SpecificFluidState, and a flow
	public final ISteamCurrentSource source;
	public List<AABB> boundList = new ArrayList<>();
	public AABB bounds;
	public Direction direction;
	public float maxDistance;
	protected List<Entity> caughtEntities = new ArrayList<>();
	protected List<Integer> caughtTurbineIds = new ArrayList<>();

	public LegacySteamCurrent(ISteamCurrentSource source) {
		this.source = source;
	}

	public List<Entity> getCaughtEntities(){
		return caughtEntities;
	}

	public List<Integer> getCaughtTurbineIds() {
		return caughtTurbineIds;
	}

	public void tick() {
		if (direction == null)
			rebuild();
		Level world = source.getSteamCurrentWorld();
		Direction facing = direction;


		tickAffectedEntities(world, facing);
	}
	protected void tickAffectedEntities(Level world, Direction facing) {

		for (int id :  caughtTurbineIds) {
			Entity entity = world.getEntity(id);
			if (entity != null)
				if (!entity.getBoundingBox()
						.intersects(bounds) && entity instanceof ControlledContraptionEntity cce && cce.getContraption() instanceof TurbineContraption turbineContraption) {
				turbineContraption.removeFromCurrents(source.getSteamCurrentPos().asLong());
				}
		}
		caughtTurbineIds.clear();
		for (Iterator<Entity> iterator = caughtEntities.iterator(); iterator.hasNext();) {//make a list of caught turbine, so we can remove the source when not colliding anymore
			Entity entity = iterator.next();
			if (entity.getBoundingBox()
					.intersects(bounds) && entity instanceof ControlledContraptionEntity cce && cce.getContraption() instanceof TurbineContraption turbineContraption){
				//float speed = Math.abs(source.getSpeed());
				turbineContraption.addToCurrents(source.getSteamCurrentPos().asLong());
				if (!caughtTurbineIds.contains(cce.getId())){
					caughtTurbineIds.add(cce.getId());
				}
			}
			else {
				iterator.remove();
            }
		}
	}

	private boolean intersectBounds(Entity entity) {
		for (AABB bounds :
				boundList) {
			if (entity.getBoundingBox()
					.intersects(bounds))
				return true;
		}
		return false;
	}
	private List<Entity> getEntities(Level level){
		ArrayList<Entity> entities = new ArrayList<>();
		for (AABB bounds :
				boundList) {
			entities.addAll(level.getEntities(null, bounds));
		}
		return entities;
	}

	//TODO redo according to my logic -> need to find the rotation axis ?
	// then when colliding with a turbine contraption -> get the turbine bearing and add to the speed
	// ( a calcul with the nbr of blades ?)
	public void rebuild() {

		direction = source.getSteamFlowOriginSide();
		maxDistance = source.getMaxDistance();

		Level world = source.getSteamCurrentWorld();
		BlockPos start = source.getSteamCurrentPos();
		float max = this.maxDistance;
		Direction facing = direction;
		Vec3 directionVec = Vec3.atLowerCornerOf(facing.getNormal());
		maxDistance = getFlowLimit(world, start, max, facing);
		int limit = getLimit();
		int searchStart = 1;
		int searchEnd = limit;
		int searchStep = 1;
		int toOffset = -1;

		if (maxDistance < 0.25f)
			bounds = new AABB(0, 0, 0, 0, 0, 0);
		else {
			float factor = maxDistance - 1;
			Vec3 scale = directionVec.scale(factor);
			if (factor > 0)
				bounds = new AABB(start.relative(direction)).expandTowards(scale);
			else {
				bounds = new AABB(start.relative(direction)).contract(scale.x, scale.y, scale.z)
						.move(scale);
			}
		}
	}

	public static float getFlowLimit(Level world, BlockPos start, float max, Direction facing) {
		Vec3 directionVec = Vec3.atLowerCornerOf(facing.getNormal());
		// add 2 to the flow if the block is a turbine blade
		// Determine the distance of the air flow
		float distance = 0;
		for (int i = 1; i <= max; i++) {
            BlockPos currentPos = start.relative(facing, i);
            if (!world.isLoaded(currentPos))
                break;
            BlockState state = world.getBlockState(currentPos);
			if (TagsInit.CustomBlockTags.TURBINE_BLADE.matches(state)){
				distance+=2;
			}
			else if (!state.isAir()){
				break;
			}
        }
		return distance;
	}

	private static boolean shouldAlwaysPass(BlockState state) {
		return AllTags.AllBlockTags.FAN_TRANSPARENT.matches(state);
	}
	private int getLimit() {
		if ((float) (int) maxDistance == maxDistance) {
			return (int) maxDistance;
		} else {
			return (int) maxDistance + 1;
		}
	}
	public void findEntities() {
		caughtEntities.clear();
		caughtEntities = source.getSteamCurrentWorld().getEntities(null, bounds);
	}

	public void remove() {
		for (int id :  caughtTurbineIds) {
			Entity entity = source.getSteamCurrentWorld().getEntity(id);
			if (entity != null)
				if (entity instanceof ControlledContraptionEntity cce && cce.getContraption() instanceof TurbineContraption turbineContraption) {
					turbineContraption.removeFromCurrents(source.getSteamCurrentPos().asLong());
				}
		}
	}
}
