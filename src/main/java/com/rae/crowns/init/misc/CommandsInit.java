package com.rae.crowns.init.misc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicThread;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.nuclear.NuclearExplosion;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;

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
                                    float        power  = FloatArgumentType.getFloat(context, "power");
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

                                    Objects.requireNonNull(PhysicsSaveManager.get((ServerLevel) player.level()))
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

                .then(Commands.literal("reinitialiseAllSection")
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            Objects.requireNonNull(PhysicsSaveManager.get(level)
                            ).reinitializeAll();
                            return Command.SINGLE_SUCCESS;
                        })

                )

                .then(Commands.literal("clearSteamCurrents")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal("Clearing "+ SteamFlowManager.getSFAmount()+ " across all dimensions"));
                            SteamFlowManager.clear();
                            return Command.SINGLE_SUCCESS;
                        })
                )

                .then(Commands.literal("recordAssembly")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).then(
                                Commands.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(
                                                context -> {


                                                    return Command.SINGLE_SUCCESS;
                                                }
                                        )
                        ))
                )
                .then(Commands.literal("dumpThermodynamicStatus")
                        .executes(context -> dumpStatus(context, false))        // no argument → defaults to false
                        .then(Commands.argument("detailed", BoolArgumentType.bool())
                                .executes(context -> dumpStatus(context, true))     // argument present → pass it
                        )
                )
        );
    }


    private static int dumpStatus(CommandContext<CommandSourceStack> context, boolean detailed) {
        boolean isDetailed = detailed && BoolArgumentType.getBool(context, "detailed");

        PhysicsWorldData data = PhysicsSaveManager.get(context.getSource().getLevel());
        if (data == null) {
            context.getSource().sendSystemMessage(Component.literal("grid not loaded"));
            return Command.SINGLE_SUCCESS;
        }

        Map<DataLayerType, List<Long>> initialise = data.remainingInitialise();
        long gameTime = context.getSource().getLevel().getGameTime();

        send(context, "___________thermodynamic simulation status___________");
        send(context, "   -" + data.getDynamicData().size() + " dynamic data blocks");
        send(context, "   -" + data.getLoadedSections().size() + " loaded chunk sections");
        send(context, "   -" + data.getLoadedSections().stream()
                .filter(section -> data.ticked(section, (int) gameTime)).toList().size() + " ticked sections");

        boolean anyPending = Stream.of(
                DataLayerType.CONDUCTION,
                DataLayerType.RESILIENCE,
                DataLayerType.TEMPERATURE,
                DataLayerType.DEFAULT_TEMPERATURE
        ).anyMatch(type -> !initialise.getOrDefault(type, new ArrayList<>()).isEmpty());

        if (anyPending) {
            send(context, "   -initialization :");
            send(context, "      -" + DataLayerType.CONDUCTION.id          + " " + initialise.getOrDefault(DataLayerType.CONDUCTION,          new ArrayList<>()).size());
            send(context, "      -" + DataLayerType.RESILIENCE.id          + " " + initialise.getOrDefault(DataLayerType.RESILIENCE,          new ArrayList<>()).size());
            send(context, "      -" + DataLayerType.TEMPERATURE.id         + " " + initialise.getOrDefault(DataLayerType.TEMPERATURE,         new ArrayList<>()).size());
            send(context, "      -" + DataLayerType.DEFAULT_TEMPERATURE.id + " " + initialise.getOrDefault(DataLayerType.DEFAULT_TEMPERATURE, new ArrayList<>()).size());
        } else {
            send(context, "   -initialization : empty");
        }
        // ── Tick stats ───────────────────────────────────────────────────────────
        send(context, "   -tick performance (last 60 s) :");
        PhysicThread thread = PhysicThread.getInstance();
        if (thread != null && thread.isRunning()) {
            double[] s = thread.stats.snapshot();
            if (s != null) {
                send(context, String.format("      -mean   : %.2f ms", s[0]));
                send(context, String.format("      -min    : %.2f ms", s[1]));
                send(context, String.format("      -max    : %.2f ms", s[2]));
                send(context, String.format("      -median : %.2f ms", s[3]));
                send(context, String.format("      -stddev : %.2f ms", s[4]));
            } else {
                send(context, "      -no data yet");
            }
        } else {
            send(context, "      -physics thread not running");
        }

        if (isDetailed) {
            for (Map.Entry<DataLayerType, List<Long>> entry : initialise.entrySet()) {
                context.getSource().sendSystemMessage(Component.literal(
                        entry.getValue().stream().map(SectionPos::of).toList().toString()
                ));
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private static void send(CommandContext<CommandSourceStack> context, String msg) {
        context.getSource().sendSystemMessage(Component.literal(msg));
    }
}
