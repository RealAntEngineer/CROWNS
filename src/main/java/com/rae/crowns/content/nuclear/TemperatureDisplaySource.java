package com.rae.crowns.content.nuclear;

import com.rae.crowns.CROWNSLang;
import com.rae.crowns.content.thermodynamics.conduction.IHaveTemperature;
import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TemperatureDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        boolean isBook = context.getTargetBlockEntity() instanceof LecternBlockEntity;

        List<MutableComponent> list = provideEntries(context, stats.maxRows() * (isBook ? ENTRIES_PER_PAGE : 1))
                .toList();

        /*if (isBook)
            list = condensePages(list);*/

        return list;
    }

    static final int ENTRIES_PER_PAGE = 8;
    @Override
    public int getPassiveRefreshTicks() {
        return 5;
    }

    protected Stream<MutableComponent> provideEntries(DisplayLinkContext context, int maxRows) {
        BlockEntity sourceBE = context.getSourceBlockEntity();
        if (!(sourceBE instanceof IHaveTemperature temperature))
            return Stream.empty();

        List<MutableComponent> values = new ArrayList<>();
        values.add(FormicApiLang.formatTemperature(temperature.getTemperature()).component());



        return values
                .stream()
                .limit(maxRows);
    }
}
