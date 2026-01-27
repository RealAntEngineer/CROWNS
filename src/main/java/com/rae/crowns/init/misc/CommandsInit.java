package com.rae.crowns.init.misc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.nuclear.NuclearExplosion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

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
                .then(Commands.literal("dumpThermodynamicStatus")
                        .executes(
                                context -> {
                                    PhysicsWorldData data = PhysicsSaveManager.get(context.getSource().getLevel());
                                    Map<DataLayerType<?>,Integer> initialise = data.remainingInitialise();
                                    context.getSource().sendSystemMessage(Component.literal(
                                            "___________thermodynamic simulation status___________\n"+
                                                    "   -"+ data.getDynamicData().size()+ " dynamic data blocks\n"+
                                                    "   -"+ data.getLoadedSections().size() + " loaded chunk sections\n"+
                                                    "   -"+ data.getLoadedSections().stream().filter(data::ticked).toList().size()+ " ticked sections\n"+
                                                    "   -initialization :\n"+
                                                    "      -"+DataLayerType.CONDUCTION.id+ " "+ initialise.getOrDefault(DataLayerType.CONDUCTION, 0)+ "\n"+
                                                    "      -"+DataLayerType.RESILIENCE.id+ " "+ initialise.getOrDefault(DataLayerType.RESILIENCE, 0)+"\n"+
                                                    "      -"+DataLayerType.TEMPERATURE.id+ " "+ initialise.getOrDefault(DataLayerType.TEMPERATURE, 0)+"\n"+
                                                    "      -"+DataLayerType.DEFAULT_TEMPERATURE.id+ " "+ initialise.getOrDefault(DataLayerType.DEFAULT_TEMPERATURE, 0)

                                    ));
                                    return Command.SINGLE_SUCCESS;
                                }
                        )
                )
        );
    }
}

