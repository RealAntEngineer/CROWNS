package com.rae.crowns.content.nuclear.fuel_rod;

import com.rae.crowns.init.misc.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FuelRodBlockEntity extends BlockEntity {

    private static final int MAX_FUEL = 64;
    private static final int BURN_INTERVAL = 20;

    private int storedFuel = 0;
    private int burnTick = 0;
    private int temperature = 20;

    public FuelRodBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.FUEL_ROD.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FuelRodBlockEntity be) {
        if (level.isClientSide) return;

        if (be.storedFuel > 0) {
            be.burnTick++;
            if (be.burnTick >= BURN_INTERVAL) {
                be.burnTick = 0;
                be.storedFuel--;
                be.temperature = Math.min(1200, be.temperature + 10);
                be.updateActivity();
                be.setChanged();
            }
        } else {
            if (be.temperature > 20) {
                be.temperature = Math.max(20, be.temperature - 4);
            }
            be.updateActivity();
        }
    }

    public boolean canAcceptFuel() {
        return storedFuel < MAX_FUEL;
    }

    public boolean addFuel(int amount) {
        if (amount <= 0 || storedFuel >= MAX_FUEL) return false;
        int newFuel = Math.min(MAX_FUEL, storedFuel + amount);
        if (newFuel == storedFuel) return false;
        storedFuel = newFuel;
        updateActivity();
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public int getStoredFuel() {
        return storedFuel;
    }

    public int getTemperature() {
        return temperature;
    }

    private void updateActivity() {
        if (level == null) return;

        FuelRodBlock.Activity next =
                storedFuel <= 0 ? FuelRodBlock.Activity.NONE :
                storedFuel < MAX_FUEL / 2 ? FuelRodBlock.Activity.LOW :
                FuelRodBlock.Activity.HIGH;

        BlockState state = getBlockState();
        if (state.getValue(FuelRodBlock.ACTIVITY) != next) {
            level.setBlock(worldPosition, state.setValue(FuelRodBlock.ACTIVITY, next), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredFuel", storedFuel);
        tag.putInt("BurnTick", burnTick);
        tag.putInt("Temperature", temperature);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedFuel = tag.getInt("StoredFuel");
        burnTick = tag.getInt("BurnTick");
        temperature = tag.getInt("Temperature");
    }
}
