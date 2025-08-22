package com.rae.crowns.init.misc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.rae.crowns.content.nuclear.NuclearExplosion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class CommandsInit {

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(Commands.literal("nuclearExplosion")
                    .requires(source -> source.hasPermission(2)) // Requires operator level permission
                    .then(Commands.argument("power", FloatArgumentType.floatArg(0.0F)) // you can set min/max here
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                float power = FloatArgumentType.getFloat(context, "power"); // <-- get the float argument
                                NuclearExplosion.nuclearExplosion(player.level(), player.getOnPos(), power);
                                return Command.SINGLE_SUCCESS;
                            })));
        }
}
