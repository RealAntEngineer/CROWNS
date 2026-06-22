package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.client.ShapesInit;
import com.rae.crowns.init.misc.BlockEntityInit;
import com.rae.crowns.init.misc.ItemInit;
import com.rae.formicapi.content.multiblock.MBKineticController;
import com.rae.formicapi.content.multiblock.MBStructureBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.system.NonnullDefault;

import java.util.Objects;

@NonnullDefault
public class TurbineStageBlock extends MBKineticController implements IBE<TurbineStageBlockEntity> {

    public static final BooleanProperty CASING = BooleanProperty.create("casing");

    public TurbineStageBlock(Properties pProperties, MBStructureBlock structure) {
        super(pProperties, structure);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(CASING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CASING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemInit.TURBINE_CASING.asItem()) && !state.getValue(CASING)) {
            BlockState newState = state.setValue(CASING, true);
            if (hasSpace(level, newState, pos, true)) {
                level.setBlock(pos, newState, 3);
                repairStructure(level, newState, pos, state.getValue(FACING));

                if (!player.isCreative())
                    stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        if (stack.isEmpty() && state.getValue(CASING)) {
            BlockState newState = state.setValue(CASING, false);
            //it shrinks so no need to check for the shape.
            level.setBlock(pos, newState, 3);
            repairStructure(level, newState, pos, state.getValue(FACING));
            if (!player.isCreative())
                player.getInventory().setPickedItem(ItemInit.TURBINE_CASING.asStack(1));
            return InteractionResult.SUCCESS;
        }
        return super.use(state, level, pos, player, hand, hitResult);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return true;
    }

    @Override
    public VoxelShape getGlobalShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return state.hasProperty(CASING) && state.getValue(CASING) ?
                ShapesInit.TURBINE.get(state.getValue(FACING)) :
                Shapes.join(ShapesInit.TURBINE.get(state.getValue(FACING)), Shapes.block(), BooleanOp.AND);
    }

    @Override
    public Vec3i getDefaultOffset(BlockState state) {
        return state.hasProperty(CASING) && state.getValue(CASING) ? new Vec3i(0, 1, 1) : new Vec3i(0, 0, 0);
    }

    @Override
    public Vec3i getDefaultSize(BlockState state) {
        return state.hasProperty(CASING) && state.getValue(CASING) ? new Vec3i(1, 3, 3) : new Vec3i(1, 1, 1);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(CASING, false);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean showCapacityWithAnnotation() {
        return true;
    }

    @Override
    public Class<TurbineStageBlockEntity> getBlockEntityClass() {
        return TurbineStageBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineStageBlockEntity> getBlockEntityType() {
        return BlockEntityInit.TURBINE_STAGE.get();
    }
}