package com.rae.crowns.content.hazards.radiation.pointsource;

import com.rae.crowns.content.hazards.HazardEntry;
import com.rae.crowns.content.hazards.ItemRadiation;
import com.rae.crowns.content.hazards.radiation.ContaminationUtil;
import com.rae.crowns.init.misc.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;

// Handles block radiation
@Mod.EventBusSubscriber
public class PointSourceHandler {
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();
        HashMap<BlockPos, HazardEntry> blocks = PointSourceUtil.findRadBlocks(player, 16);

        for (BlockPos pos : blocks.keySet().stream().toList()) {
            ArrayList<BlockPos> positions = PointSourceUtil.getBresenhamLine(player.blockPosition(), pos);
            boolean blocked = false;

            for (BlockPos pos2 : positions) {
                if (level.getBlockState(pos2).is(BlockInit.REACTOR_CASING.get())) blocked = true;
            }
            if (blocked) continue;

            HazardEntry entry = blocks.get(pos);
            ItemRadiation.DecayContainer container = entry.container;

            Vec3 vecDistance = player.position().subtract(pos.getCenter());
            double distance = vecDistance.length();

            ContaminationUtil.addContamination(player, (container.getReontgen(distance) * 0.0096) / 20);
        }
    }
}