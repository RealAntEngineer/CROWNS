package com.rae.crowns.content.nuclear;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssemblyBlock extends RotatedPillarBlock implements IBE<AssemblyBlockEntity> {
    public static final EnumProperty<Temperature> TEMPERATURE = EnumProperty.create("temperature", Temperature.class); //T*10
    public static final EnumProperty<Activity> ACTIVITY = EnumProperty.create("activity", Activity.class);

    public AssemblyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(TEMPERATURE, Temperature.COLD)
                .setValue(ACTIVITY, Activity.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : ($0, pos, $1, blockEntity) -> {
            if (blockEntity instanceof AssemblyBlockEntity assemblyBlockEntity) {
                assemblyBlockEntity.tick();
            }
        };
    }

    @Override
    @SuppressWarnings("deprecated")
    public int getSignal(@NotNull BlockState state, BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        if (level.getBlockEntity(pos) instanceof AssemblyBlockEntity assemblyBlockEntity) {
            return (int) (assemblyBlockEntity.getTemperature() / 3500f * 16f);
        }
        return super.getSignal(state, level, pos, direction);
    }

    @Override
    @SuppressWarnings("deprecated")
    public void onRemove(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pNewState, boolean pIsMoving) {
        IBE.onRemove(pState, pLevel, pPos, pNewState);
    }
    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity player, @NotNull ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, player, itemStack);
        if (level.isClientSide)
            return;
        withBlockEntityDo(level, pos, be -> {
            be.setComposition(itemStack.getOrCreateTag().getCompound("composition"));
        });
    }

    public enum Activity implements StringRepresentable {
        NONE, LOW, HIGH;

        @Override
        public @NotNull String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    public enum Temperature implements StringRepresentable {
        COLD, WARM, HOT;

        @Override
        public @NotNull String getSerializedName() {
            return this.name().toLowerCase();
        }
    }
}
