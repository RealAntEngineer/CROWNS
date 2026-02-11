package com.rae.crowns.content.nuclear.display;

import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlock;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FuelStackDisplaySource extends DisplaySource {
    static final int ENTRIES_PER_PAGE = 8;

    @Override
    public @NotNull List<MutableComponent> provideText(@NotNull DisplayLinkContext context, @NotNull DisplayTargetStats stats) {
        boolean isBook = context.getTargetBlockEntity() instanceof LecternBlockEntity;

        return provideEntries(context, stats.maxRows() * (isBook ? ENTRIES_PER_PAGE : 1))
                .toList();
    }
    private static List<AssemblyBlockEntity> collectFuelStack(
            Level level,
            BlockPos startPos,
            Direction.Axis axis
    ) {
        List<AssemblyBlockEntity> stack = new ArrayList<>();

        // Choose directions based on axis
        Direction negative = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        Direction positive = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);

        // Walk down/backwards
        BlockPos.MutableBlockPos cursor = startPos.mutable();
        while (true) {
            BlockEntity be = level.getBlockEntity(cursor);
            if (!(be instanceof AssemblyBlockEntity assembly))
                break;

            stack.add(assembly);
            cursor.move(negative);
        }

        // Walk up/forwards (skip origin to avoid duplicate)
        cursor.set(startPos).move(positive);
        while (true) {
            BlockEntity be = level.getBlockEntity(cursor);
            if (!(be instanceof AssemblyBlockEntity assembly))
                break;

            stack.add(assembly);
            cursor.move(positive);
        }

        return stack;
    }
    @Override
    public int getPassiveRefreshTicks() {
        return 5;
    }

    protected @NotNull Stream<MutableComponent> provideEntries(
            @NotNull DisplayLinkContext context,
            int maxRows
    ) {
        BlockEntity sourceBE = context.getSourceBlockEntity();
        if (!(sourceBE instanceof AssemblyBlockEntity assembly))
            return Stream.empty();

        Level level = assembly.getLevel();
        if (level == null)
            return Stream.empty();

        List<AssemblyBlockEntity> stack =
                collectFuelStack(level, assembly.getBlockPos(), assembly.getBlockState().getValue(AssemblyBlock.AXIS));

        List<MutableComponent> values = new ArrayList<>();

        for (AssemblyBlockEntity part : stack) {
            values.add(FormicApiLang.formatTemperature(part.getTemperature()).component());
        }

        return values.stream().limit(maxRows);
    }
}
