package com.rae.crowns.init.misc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
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

        // Root command: /crowns
        dispatcher.register(Commands.literal("crowns")
                .requires(source -> source.hasPermission(2)) // Operator permission for all subcommands

                // /crowns nuclearExplosion <power>
                .then(Commands.literal("nuclearExplosion")
                        .then(Commands.argument("power", FloatArgumentType.floatArg(0.0F))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    float power = FloatArgumentType.getFloat(context, "power");
                                    NuclearExplosion.nuclearExplosion(
                                            player.level(),
                                            player.getOnPos(),
                                            power
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                // /crowns reinitialiseSection <pos>
                .then(Commands.literal("reinitialiseSection")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long sectionPos = SectionPos.of(
                                            BlockPosArgument.getBlockPos(context, "pos")
                                    ).asLong();

                                    PhysicsSaveManager.get((ServerLevel) player.level())
                                            .scheduleInitialisation(
                                                    sectionPos,
                                                    DataLayerType.CONDUCTION,
                                                    DataLayerType.DEFAULT_TEMPERATURE,
                                                    DataLayerType.TEMPERATURE,
                                                    DataLayerType.RESILIENCE
                                            );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                .then(Commands.literal("recordAssembly")
                        .then(Commands.argument("pos",  BlockPosArgument.blockPos()).then(
                                Commands.argument("ticks",IntegerArgumentType.integer(0))
                                        .executes(
                                                context -> {


                                                    return Command.SINGLE_SUCCESS;
                                                }
                                        )


                        ))



                )
        );
    }
}

