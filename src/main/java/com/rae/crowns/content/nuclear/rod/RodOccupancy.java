package com.rae.crowns.content.nuclear.rod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared bookkeeping for any {@link IRodContainerBlockEntity}: tracks how many meters of which
 * {@link RodBlock} type(s) currently pass through the owning block.
 * <p>
 * A hollow block is 1 meter long, so the combined occupancy across all entries never exceeds
 * 1 meter. In practice at most 2 entries should ever exist at once - e.g. the tail of one rod
 * leaving a sleeve while the head of a different rod enters it from the opposite face.
 */
public class RodOccupancy {

    public static final float CAPACITY = 1f;

    private final List<Entry> entries = new ArrayList<>(2);

    /**
     * @param block  the rod whose occupancy is changing
     * @param amount meters to add (positive) or remove (negative)
     * @return the amount actually applied, after clamping to the available capacity. A return
     *         value of 0 when inserting means there was no room left for this rod.
     */
    public float insert(RodBlock block, float amount) {
        if (amount == 0)
            return 0;

        Entry entry = find(block);

        if (entry == null) {
            if (amount <= 0)
                return 0; // nothing to remove, this rod isn't present here

            if (entries.size() >= 2)
                return 0; // no room for a third distinct rod

            entry = new Entry(block, 0);
            entries.add(entry);
        }

        float before = entry.amount;
        float room = CAPACITY - (getTotalOccupied() - before);
        entry.amount = Mth.clamp(entry.amount + amount, 0, Math.max(room, 0));

        if (entry.amount <= 0)
            entries.remove(entry);

        return entry.amount - before;
    }

    public float getTotalOccupied() {
        float total = 0;
        for (Entry entry : entries)
            total += entry.amount;
        return total;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public float getAbsorption() {
        float sum = 0;
        for (Entry entry : entries)
            sum += entry.block.getAbsorption() * entry.amount;
        return sum;
    }

    public float getModeration() {
        float sum = 0;
        for (Entry entry : entries)
            sum += entry.block.getModeration() * entry.amount;
        return sum;
    }

    public float getReflection() {
        float sum = 0;
        for (Entry entry : entries)
            sum += entry.block.getReflection() * entry.amount;
        return sum;
    }

    private Entry find(RodBlock block) {
        for (Entry entry : entries)
            if (entry.block == block)
                return entry;
        return null;
    }

    public void write(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(entry.block);
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Rod", key.toString());
            entryTag.putFloat("Amount", entry.amount);
            list.add(entryTag);
        }
        tag.put("Occupancy", list);
    }

    public void read(CompoundTag tag) {
        entries.clear();
        if (!tag.contains("Occupancy"))
            return;

        ListTag list = tag.getList("Occupancy", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            ResourceLocation key = ResourceLocation.tryParse(entryTag.getString("Rod"));
            if (key == null)
                continue;

            Block block = BuiltInRegistries.BLOCK.get(key);
            if (block instanceof RodBlock rodBlock)
                entries.add(new Entry(rodBlock, entryTag.getFloat("Amount")));
        }
    }

    private static class Entry {
        final RodBlock block;
        float amount;

        Entry(RodBlock block, float amount) {
            this.block = block;
            this.amount = amount;
        }
    }
}