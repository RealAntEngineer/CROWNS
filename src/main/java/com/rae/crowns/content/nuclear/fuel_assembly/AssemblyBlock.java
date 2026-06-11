package com.rae.crowns.content.nuclear.fuel_assembly;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.rae.crowns.init.misc.BlockInit;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PoleHelper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NonnullDefault;

import java.util.Optional;
import java.util.function.Predicate;

@NonnullDefault
public class AssemblyBlock extends RotatedPillarBlock implements IBE<AssemblyBlockEntity> {
    public static final EnumProperty<Temperature> TEMPERATURE = EnumProperty.create("temperature", Temperature.class); //T*10
    public static final EnumProperty<Activity> ACTIVITY = EnumProperty.create("activity", Activity.class);

    private static final int placementHelperId = PlacementHelpers.register(new AssemblyBlock.PlacementHelper());

    public AssemblyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(TEMPERATURE, Temperature.COLD)
                .setValue(ACTIVITY, Activity.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(TEMPERATURE, ACTIVITY);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<AssemblyBlockEntity> getBlockEntityClass() {
        return AssemblyBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AssemblyBlockEntity> getBlockEntityType() {
        return BlockEntityInit.FUEL_ASSEMBLY.get();
    }

    @Override
    @SuppressWarnings("deprecated")
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        IBE.onRemove(pState, pLevel, pPos, pNewState);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult ray) {
        ItemStack heldItem = player.getItemInHand(hand);

        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(heldItem)) {
                placementHelper.getOffset(player, world, state, pos, ray)
                        .placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecated")
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof AssemblyBlockEntity assemblyBlockEntity) {
            return (int) (assemblyBlockEntity.getTemperature() / 3500f * 16f);
        }
        return super.getSignal(state, level, pos, direction);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, player, itemStack);
        if (level.isClientSide)
            return;
        withBlockEntityDo(level, pos, be -> {
            be.setComposition(itemStack.getOrCreateTag().getCompound("composition"));
        });
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter blockGetter, BlockPos pos, BlockState state) {
        Item item = asItem();

        Optional<AssemblyBlockEntity> blockEntityOptional = getBlockEntityOptional(blockGetter, pos);
        CompoundTag composition = blockEntityOptional.map(AssemblyBlockEntity::saveComposition)
                .map(CompoundTag::copy)
                .orElse(new CompoundTag());

        ItemStack stack = new ItemStack(item, 1);
        CompoundTag compoundtag = stack.getOrCreateTag();
        compoundtag.put("composition", composition);
        stack.setTag(compoundtag);
        return stack;
    }


    public enum Activity implements StringRepresentable {
        NONE, LOW, HIGH;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    public enum Temperature implements StringRepresentable {
        COLD, WARM, HOT;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper extends PoleHelper<Direction.Axis> {
        private PlacementHelper() {
            super(state -> state.getBlock() instanceof AssemblyBlock, state -> state.getValue(AXIS), AXIS);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return BlockInit.FUEL_ASSEMBLY::isIn;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof AssemblyBlock;
        }

    }
}
