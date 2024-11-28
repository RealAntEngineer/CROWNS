package com.rae.crowns.content.legacy;

import com.rae.crowns.init.CROWNSContraptionType;
import com.rae.crowns.init.TagsInit;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.bearing.AnchoredLighter;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

public class TurbineContraption extends Contraption {

	protected int bladeBlocks;
	private ArrayList<Long> currents = new ArrayList<>();
	protected Direction facing;
	private boolean isTurbine;
	private int nbrOfStages;

	public TurbineContraption() {}

	public TurbineContraption(Direction facing) {
		this.isTurbine = true;
		this.facing = facing;
	}

	@Override
	public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
		BlockPos offset = pos.relative(facing);
		if (!searchMovedStructure(world, offset, null))
			return false;
		startMoving(world);
		expandBoundsAroundAxis(facing.getAxis());
		if (isTurbine && bladeBlocks < AllConfigs.server().kinetics.minimumWindmillSails.get())
			throw AssemblyException.notEnoughSails(bladeBlocks);
        return !blocks.isEmpty();
    }

	@Override
	public ContraptionType getType() {
		return CROWNSContraptionType.TURBINE;
	}

	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return pos.equals(anchor.relative(facing.getOpposite()));
	}

	@Override
	public void addBlock(BlockPos pos, Pair<StructureBlockInfo, BlockEntity> capture) {
		BlockPos localPos = pos.subtract(anchor);
		if (!getBlocks().containsKey(localPos) && TagsInit.CustomBlockTags.TURBINE_BLADE.matches(getBladeBlock(capture)))
			bladeBlocks++;
		super.addBlock(pos, capture);
	}

	private BlockState getBladeBlock(Pair<StructureBlockInfo, BlockEntity> capture) {
		return capture.getKey().state;
	}

	@Override
	public CompoundTag writeNBT(boolean spawnPacket) {
		CompoundTag tag = super.writeNBT(spawnPacket);
		tag.putInt("Blades", bladeBlocks);
		tag.putInt("Facing", facing.get3DDataValue());
		tag.putLongArray("currents",currents);
		return tag;
	}

	@Override
	public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
		bladeBlocks = tag.getInt("Blades");
		facing = Direction.from3DDataValue(tag.getInt("Facing"));
		currents = fromLong(tag.getLongArray("currents"));
		super.readNBT(world, tag, spawnData);
	}

	public void addToCurrents(Long pos){
		if (!currents.contains(pos)){
			currents.add(pos);
		}
	}
	public void removeFromCurrents(long pos) {
		currents.remove(pos);
	}

	public ArrayList<Long> getCurrents() {
		return currents;
	}

	private ArrayList<Long> fromLong(long[] longs) {
		ArrayList<Long> currents = new ArrayList<>();
		for (long l:
			 longs) {
			currents.add(l);
		}
		return currents;
	}

	public int getBladeBlocks() {
		return bladeBlocks;
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	public boolean canBeStabilized(Direction facing, BlockPos localPos) {
		if (facing.getOpposite() == this.facing && BlockPos.ZERO.equals(localPos))
			return false;
		return facing.getAxis() == this.facing.getAxis();
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public ContraptionLighter<?> makeLighter() {
		return new AnchoredLighter(this);
	}


}
