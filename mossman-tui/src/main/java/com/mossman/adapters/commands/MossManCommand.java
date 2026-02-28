package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class MossManCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("mossman")
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("MossMan Issue Tracker. Use /mossman project list"));
                            return 1;
                        })
        );

        ProjectCommand.register(dispatcher);
        TicketCommand.register(dispatcher);
        MailCommand.register(dispatcher);
        SettingsCommand.register(dispatcher);
    }
}
