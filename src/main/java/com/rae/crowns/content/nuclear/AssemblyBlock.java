package com.rae.crowns.content.nuclear;

import com.rae.crowns.init.BlockEntityInit;
import com.rae.crowns.content.legacy.RealWorkingFluid;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class AssemblyBlock extends RotatedPillarBlock implements IBE<AssemblyBlockEntity> {

    public static final EnumProperty<RealWorkingFluid.Temperature> TEMPERATURE = EnumProperty.create("temperature", RealWorkingFluid.Temperature.class); //T*10
    public static final EnumProperty<Activity> ACTIVITY = EnumProperty.create("activity", Activity.class);

    public AssemblyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(TEMPERATURE, RealWorkingFluid.Temperature.COLD)
                .setValue(ACTIVITY,Activity.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TEMPERATURE,ACTIVITY);
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
        return level.isClientSide() ? null : ($0,pos,$1,blockEntity) -> {
            if(blockEntity instanceof AssemblyBlockEntity assemblyBlockEntity) {
                assemblyBlockEntity.tick();
            }
        };
    }

    public enum Activity implements StringRepresentable {
        NONE,LOW,HIGH;
        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }
    public enum Temperature implements StringRepresentable {
        COLD,WARM,HOT;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

}
