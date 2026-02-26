package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ProjectCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var projectNode = CommandManager.literal("project");
        ProjectCoreCommands.register(projectNode);
        ProjectMemberCommands.register(projectNode);
        ProjectStatusCommands.register(projectNode);
        ProjectTicketTypeCommands.register(projectNode);
        ProjectRelationshipTypeCommands.register(projectNode);
        rootNode.addChild(projectNode.build());
    }
}
