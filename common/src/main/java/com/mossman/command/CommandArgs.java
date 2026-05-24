package com.mossman.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Brigadier argument builders for the {@code /mossman} command tree. Each
 * returns a freshly-built {@link RequiredArgumentBuilder} so callers can
 * attach {@code .then(...)} / {@code .executes(...)} without sharing state.
 * The suggestion providers in {@link Suggestions} look up the previously-typed
 * {@code projectId} from the command context, so {@link #projectIdArg()} must
 * appear before any of the project-scoped arg builders in the tree.
 */
final class CommandArgs {

    private CommandArgs() {}

    static RequiredArgumentBuilder<CommandSourceStack, String> projectIdArg() {
        return Commands.argument("projectId", StringArgumentType.word())
                .suggests(Suggestions.PROJECT_IDS);
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> roleIdArg() {
        return Commands.argument("roleId", StringArgumentType.string())
                .suggests(Suggestions.roleNames("projectId"));
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> statusIdArg() {
        return Commands.argument("statusId", StringArgumentType.string())
                .suggests(Suggestions.statusNames("projectId"));
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> typeIdArg() {
        return Commands.argument("typeId", StringArgumentType.string())
                .suggests(Suggestions.typeNames("projectId"));
    }

    static RequiredArgumentBuilder<CommandSourceStack, Integer> ticketIdArg() {
        return Commands.argument("ticketId", IntegerArgumentType.integer(1))
                .suggests(Suggestions.ticketNumbers("projectId"));
    }
}
