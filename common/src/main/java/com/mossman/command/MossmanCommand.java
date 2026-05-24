package com.mossman.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Chat surface entry point for every core use case, shaped per
 * {@code docs/commands.md}. Top-level layout:
 *
 * <pre>
 *   /mossman project ... (list, view, create, delete, update, config{role,status,type})
 *   /mossman ticket ...  (list, view, create, delete, update)
 * </pre>
 *
 * The per-domain handlers live in {@link ProjectCommands}, {@link RoleCommands},
 * {@link StatusCommands}, {@link TypeCommands}, {@link TicketCommands}. Shared
 * infrastructure lives in {@link CommandArgs}, {@link Resolvers}, and
 * {@link Mutations}. Tab completion comes from {@link Suggestions}; SNBT patch
 * parsing from {@link SnbtPatch}; chat-component helpers from {@link ChatHelpers}.
 *
 * <p>This file owns only the top-level subtree composition so the {@code /mossman}
 * shape is visible at a glance — `config` is wired here so role/status/type don't
 * each have to know they live under `project`.
 */
public final class MossmanCommand {

    private MossmanCommand() {}

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) ->
                dispatcher.register(Commands.literal("mossman")
                        .then(projectSubtree())
                        .then(TicketCommands.subtree())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> projectSubtree() {
        return ProjectCommands.subtree()
                .then(Commands.literal("config")
                        .then(RoleCommands.subtree())
                        .then(StatusCommands.subtree())
                        .then(TypeCommands.subtree()));
    }
}
