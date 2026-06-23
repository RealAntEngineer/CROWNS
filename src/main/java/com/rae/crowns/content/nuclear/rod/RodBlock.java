package com.rae.crowns.content.nuclear.rod;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class RodBlock extends RotatedPillarBlock implements ProperWaterloggedBlock, IBE<RodBlockEntity> {

    // Radiation properties of this rod's material, expressed per meter of rod (i.e. per block it
    // fully occupies). These feed into the weighted totals computed by hollow blocks (sleeves,
    // drivers) via RodOccupancy.
    private final float        absorption;
    private final float        moderation;
    private final float        reflection;
    private final PartialModel rodModel;

    public RodBlock(Properties properties, PartialModel rodModel, float absorption, float moderation, float reflection) {
        super(properties);
        this.absorption = absorption;
        this.moderation = moderation;
        this.reflection = reflection;
        this.rodModel = rodModel;
    }


    public PartialModel getRodModel() {
        return rodModel;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {

        if(!level.isClientSide) {
            withBlockEntityDo(level, pos, (be) -> {
                        if (player.isShiftKeyDown()) {
                            be.setOffset(be.offset - 0.1f);
                        } else {
                            be.setOffset(be.offset + 0.1f);
                        }
                    }
            );
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AllShapes.SIX_VOXEL_POLE.get(state.getValue(RotatedPillarBlock.AXIS));
    }

    @Override
    public Class<RodBlockEntity> getBlockEntityClass() {
        return RodBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RodBlockEntity> getBlockEntityType() {
        return BlockEntityInit.REACTOR_ROD.get();
    }

    /**
     * @return the fraction of incoming radiation this rod absorbs, per meter of rod.
     */
    public float getAbsorption() {
        return absorption;
    }

    /**
     * @return the fraction of incoming radiation this rod moderates, per meter of rod.
     */
    public float getModeration() {
        return moderation;
    }

    /**
     * @return the fraction of incoming radiation this rod reflects, per meter of rod.
     */
    public float getReflection() {
        return reflection;
    }
}