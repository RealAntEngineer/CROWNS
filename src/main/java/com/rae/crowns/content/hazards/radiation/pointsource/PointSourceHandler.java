package com.rae.crowns.content.hazards.radiation.pointsource;

import com.rae.crowns.content.hazards.HazardEntry;
import com.rae.crowns.content.hazards.radiation.ItemRadiation;
import com.rae.crowns.content.hazards.radiation.contamination.ContaminationUtil;
import com.rae.crowns.init.misc.BlockInit;
import com.rae.crowns.init.misc.TagsInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

// Handles block radiation
@Mod.EventBusSubscriber
public class PointSourceHandler {

    /**
     * Emits and applies radiation from a block using a SpecialContainer to all players within 64 blocks
     *
     * @param level     Level where it does the thing
     * @param origin    BlockPos from which radiation is being emitted
     * @param container SpecialContainer for the radiation event
     */
    public static void emitRadiation(@NotNull Level level, BlockPos origin, ItemRadiation.SpecialContainer container) {
        for (Player player : level.getServer().getPlayerList().getPlayers()) {
            Vec3 vecDistance = player.position().subtract(origin.getCenter());
            double distance = vecDistance.length();
            if (distance >= 128) continue;

            ArrayList<BlockPos> positions = PointSourceUtil.getBresenhamLine(player.blockPosition(), origin);
            boolean blocked = false;

            for (BlockPos pos : positions) {
                if (TagsInit.CustomBlockTags.SHIELDING.matches(level.getBlockState(pos))) blocked = true;
            }
            if (blocked) continue;

            ContaminationUtil.addContamination(player, (container.getRoentgen(distance) * 0.0096) / 20);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();
        HashMap<BlockPos, HazardEntry> blocks = PointSourceUtil.findRadBlocks(player, 16);

        for (BlockPos pos : blocks.keySet().stream().toList()) {
            ArrayList<BlockPos> positions = PointSourceUtil.getBresenhamLine(player.blockPosition(), pos);
            boolean blocked = false;

            for (BlockPos pos2 : positions) {
                if (TagsInit.CustomBlockTags.SHIELDING.matches(level.getBlockState(pos))) blocked = true;
            }
            if (blocked) continue;

            HazardEntry entry = blocks.get(pos);
            ItemRadiation.DecayContainer container = entry.container;

            Vec3 vecDistance = player.position().subtract(pos.getCenter());
            double distance = vecDistance.length();

            ContaminationUtil.addContamination(player, (container.getRoentgen(distance) * 0.0096) / 20);
        }
    }
}