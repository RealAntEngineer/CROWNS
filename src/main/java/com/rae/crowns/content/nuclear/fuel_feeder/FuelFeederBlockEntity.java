package com.rae.crowns.content.nuclear.fuel_feeder;

import com.rae.crowns.content.nuclear.fuel_rod.FuelRodBlockEntity;
import com.rae.crowns.init.misc.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FuelFeederBlockEntity extends BlockEntity {

    private static final int TRANSFER_INTERVAL = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isFuelItem(stack);
        }
    };

    private LazyOptional<IItemHandler> lazyInventory = LazyOptional.empty();
    private int transferCooldown = 0;

    public FuelFeederBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.FUEL_FEEDER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FuelFeederBlockEntity be) {
        if (level.isClientSide) return;

        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }

        BlockEntity below = level.getBlockEntity(pos.below());
        if (!(below instanceof FuelRodBlockEntity rod)) {
            be.transferCooldown = 10;
            return;
        }

        ItemStack stack = be.inventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            be.transferCooldown = 10;
            return;
        }

        if (rod.canAcceptFuel() && rod.addFuel(1)) {
            be.inventory.extractItem(0, 1, false);
            be.setChanged();
            be.transferCooldown = TRANSFER_INTERVAL;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(pos, state, state, 3);
            }
        } else {
            be.transferCooldown = 10;
        }
    }

    public boolean tryInsertFromPlayer(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !isFuelItem(held)) return false;

        ItemStack single = held.copy();
        single.setCount(1);

        ItemStack remainder = inventory.insertItem(0, single, false);
        if (remainder.isEmpty()) {
            held.shrink(1);
            setChanged();
            return true;
        }
        return false;
    }

    private static boolean isFuelItem(ItemStack stack) {
        return stack.getDescriptionId().contains("uranium");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyInventory = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyInventory.invalidate();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyInventory.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("TransferCooldown", transferCooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        transferCooldown = tag.getInt("TransferCooldown");
    }
}
