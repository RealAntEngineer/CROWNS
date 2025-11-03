package com.rae.crowns.init.misc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.nuclear.NuclearExplosion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class CommandsInit {

        public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(Commands.literal("nuclearExplosion")
                    .requires(source -> source.hasPermission(2)) // Requires operator level permission
                    .then(Commands.argument("power", FloatArgumentType.floatArg(0.0F)) // you can set min/max here
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                float power = FloatArgumentType.getFloat(context, "power"); // <-- get the float argument
                                NuclearExplosion.nuclearExplosion(player.level(), player.getOnPos(), power);
                                return Command.SINGLE_SUCCESS;
                            })));

            dispatcher.register(Commands.literal("reinitialiseSection")
                    .requires(source -> source.hasPermission(2)) // Requires operator level permission
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                long sectionPos = SectionPos.of(BlockPosArgument.getBlockPos(context, "pos")).asLong(); // <-- get the float argument
                                TemperatureManager.get((ServerLevel) player.level()).putForInitialisation(sectionPos);
                                return Command.SINGLE_SUCCESS;
                            })));
        }
}
