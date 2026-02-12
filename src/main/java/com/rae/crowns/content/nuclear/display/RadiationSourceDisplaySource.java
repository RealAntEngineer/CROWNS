package com.rae.crowns.content.nuclear.display;

import com.rae.crowns.CROWNSLang;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.nuclear.IAmRadioactiveSource;
import com.rae.formicapi.FormicApiLang;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;

import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RadiationSourceDisplaySource extends DisplaySource {
    public static final List<MutableComponent> notEnoughSpaceSingle =
            List.of(CROWNSLang.translate("display_source.radiation_source.not_enough_space")
                    .add(CROWNSLang.translate("display_source.radiation_source.for_activity_status")).component());

    public static final List<MutableComponent> notEnoughSpaceDouble =
            List.of(CROWNSLang.translate("display_source.radiation_source.not_enough_space").component(),
                    CROWNSLang.translate("display_source.radiation_source.for_activity_status").component());

    public static final List<List<MutableComponent>> notEnoughSpaceFlap =
            List.of(List.of(CROWNSLang.translate("display_source.radiation_source.not_enough_space").component()),
                    List.of(CROWNSLang.translate("display_source.radiation_source.for_activity_status").component()));
    static final int ENTRIES_PER_PAGE = 8;

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        boolean isBook = context.getTargetBlockEntity() instanceof LecternBlockEntity;

        List<MutableComponent> list = provideEntries(context, stats.maxRows() * (isBook ? ENTRIES_PER_PAGE : 1))
                .toList();


        return list;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 5;
    }

    @Override
    public List<List<MutableComponent>> provideFlapDisplayText(DisplayLinkContext context, DisplayTargetStats stats) {
        return super.provideFlapDisplayText(context, stats);
    }

    protected @NotNull Stream<MutableComponent> provideEntries(@NotNull DisplayLinkContext context, int maxRows) {
        BlockEntity sourceBE = context.getSourceBlockEntity();
        if (!(sourceBE instanceof IAmRadioactiveSource radioactiveSource))
            return Stream.empty();

        List<MutableComponent> values = new ArrayList<>();
        values.add(FormicApiLang.formatRadiationFlux(radioactiveSource.getRadioactiveActivity() * 20).component());//the radiation flux is in /ticks and we are displaying per sec
        if (sourceBE instanceof IAmFissileMaterial fissileMaterial) {
            values.add(CROWNSLang.translate("display_source.radiation_source.k_eff").text(String.valueOf(fissileMaterial.getEffectiveK())).component());
        }

        return values
                .stream()
                .limit(maxRows);
    }
}
