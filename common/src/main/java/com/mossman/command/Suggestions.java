package com.mossman.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.model.TicketType;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.server.ServerProjects;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Brigadier suggestion providers that read the live project from
 * {@link ServerProjects} and the previously-typed {@code projectId} arg
 * from the command context. Strings with spaces are auto-quoted so the
 * suggestion is directly insertable; resolution back to a value strips
 * the quotes via {@link StringArgumentType}'s normal parsing.
 */
public final class Suggestions {

    private Suggestions() {}

    /** Lists every project the calling player can VIEW. */
    public static final SuggestionProvider<CommandSourceStack> PROJECT_IDS = (ctx, builder) -> {
        UUID actor = actorUuid(ctx);
        if (actor == null) return builder.buildFuture();
        JsonProjectRepository repo = repo();
        if (repo == null) return builder.buildFuture();
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Project p : repo.list()) {
            if (!PermissionEvaluator.has(p, actor, Permission.VIEW_PROJECT)) continue;
            if (p.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(p.id(), Component.literal(p.name()));
            }
        }
        return builder.buildFuture();
    };

    public static SuggestionProvider<CommandSourceStack> roleNames(String projectIdArg) {
        return (ctx, builder) -> {
            project(ctx, projectIdArg).ifPresent(p -> {
                String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
                for (Role r : p.roles()) {
                    String s = quoteIfNeeded(r.name());
                    if (s.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        builder.suggest(s);
                    }
                }
            });
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> statusNames(String projectIdArg) {
        return (ctx, builder) -> {
            project(ctx, projectIdArg).ifPresent(p -> {
                String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
                for (TicketStatus s : p.statuses()) {
                    String q = quoteIfNeeded(s.name());
                    if (q.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        builder.suggest(q);
                    }
                }
            });
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> typeNames(String projectIdArg) {
        return (ctx, builder) -> {
            project(ctx, projectIdArg).ifPresent(p -> {
                String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
                for (TicketType t : p.types()) {
                    String q = quoteIfNeeded(t.name());
                    if (q.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        builder.suggest(q);
                    }
                }
            });
            return builder.buildFuture();
        };
    }

    /** Each ticket-number suggestion shows the ticket's title in the tooltip. */
    public static SuggestionProvider<CommandSourceStack> ticketNumbers(String projectIdArg) {
        return (ctx, builder) -> {
            project(ctx, projectIdArg).ifPresent(p -> {
                String prefix = builder.getRemaining();
                for (Ticket t : p.tickets()) {
                    String s = Integer.toString(t.number());
                    if (s.startsWith(prefix)) {
                        builder.suggest(t.number(), Component.literal(t.title()));
                    }
                }
            });
            return builder.buildFuture();
        };
    }

    // ---- internals ------------------------------------------------------

    private static Optional<Project> project(CommandContext<CommandSourceStack> ctx, String projectIdArg) {
        String projectId;
        try {
            projectId = StringArgumentType.getString(ctx, projectIdArg);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        JsonProjectRepository repo = repo();
        if (repo == null) return Optional.empty();
        return repo.find(projectId);
    }

    private static JsonProjectRepository repo() {
        return ServerProjects.repository().orElse(null);
    }

    private static UUID actorUuid(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        return p == null ? null : p.getUUID();
    }

    private static String quoteIfNeeded(String s) {
        if (s.indexOf(' ') < 0 && s.indexOf('"') < 0) return s;
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
