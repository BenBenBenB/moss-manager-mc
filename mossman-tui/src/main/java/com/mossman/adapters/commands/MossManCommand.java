package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MossManCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> rootNode = dispatcher.register(
                literal("mossman")
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("MossMan Issue Tracker. Use /mossman project list"));
                            return 1;
                        })
        );
        
        ProjectCommand.register(dispatcher, rootNode);
        TicketCommand.register(dispatcher, rootNode);
        MailCommand.register(dispatcher, rootNode);
        SettingsCommand.register(dispatcher, rootNode);
        AdminCommand.register(dispatcher, rootNode);
    }
}
