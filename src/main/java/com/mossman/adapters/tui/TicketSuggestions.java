package com.mossman.adapters.tui;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mossman.MossManMod;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Project;
import net.minecraft.server.command.ServerCommandSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TicketSuggestions {

    private TicketSuggestions() {}

    /** Suggests all ticket keys (e.g. MOSS-1) across all projects the caller can view. */
    public static CompletableFuture<Suggestions> suggestTicketKeys(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        var source = ctx.getSource();
        var keys = MossManMod.getProjectRepository()
                .findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> PermissionChecker.canView(p, source))
                .flatMap(p -> MossManMod.getTicketRepository()
                        .findByProjectId(p.getId(), 0, Integer.MAX_VALUE).stream()
                        .map(t -> t.getUserFriendlyKey(p.getTicketPrefix())));
        return SuggestionHelper.suggest(builder, keys);
    }

    /**
     * Returns a provider that suggests the current assignee names for a ticket,
     * identified by the already-parsed {@code keyArgName} argument (e.g. "MOSS-1").
     * Useful for the {@code ticket unassign} command.
     */
    public static SuggestionProvider<ServerCommandSource> suggestTicketAssignees(String keyArgName) {
        return (ctx, builder) -> {
            try {
                String key = StringArgumentType.getString(ctx, keyArgName);
                String[] parts = key.split("-");
                if (parts.length < 2) return builder.buildFuture();
                String prefix = parts[0];
                int number = Integer.parseInt(parts[1]);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                var ticket = MossManMod.getTicketRepository()
                        .findByProjectId(project.getId(), 0, Integer.MAX_VALUE).stream()
                        .filter(t -> t.getTicketNumber() == number)
                        .findFirst().orElse(null);
                if (ticket == null) return builder.buildFuture();
                var server = ctx.getSource().getServer();
                var names = ticket.getAssignees().stream()
                        .map(uuid -> {
                            var m = project.getMembers().stream()
                                    .filter(mm -> mm.uuid().equals(uuid)).findFirst();
                            if (m.isPresent()) return m.get().username();
                            var online = server.getPlayerManager().getPlayer(uuid);
                            return online != null ? online.getName().getString() : uuid.toString();
                        });
                return SuggestionHelper.suggest(builder, names);
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /**
     * Returns a filter suggestion provider for {@code ticket list}; reads project via {@code prefixArgName}.
     * Suggests SNBT with filter keys: title, priority, status, type (label omitted as it requires list syntax).
     */
    public static SuggestionProvider<ServerCommandSource> suggestTicketFilter(String prefixArgName) {
        return (ctx, builder) -> {
            try {
                String prefix = StringArgumentType.getString(ctx, prefixArgName);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                return SuggestionHelper.generatePatchSuggestions(builder, SuggestionHelper.parsePatchCursor(builder.getRemaining()),
                        buildTicketFilterFields(project));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /** Returns a patch suggestion provider for {@code ticket update}; reads project via {@code keyArgName}. */
    public static SuggestionProvider<ServerCommandSource> suggestTicketPatch(String keyArgName) {
        return (ctx, builder) -> {
            try {
                String key = StringArgumentType.getString(ctx, keyArgName);
                String prefix = key.split("-")[0];
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                return SuggestionHelper.generatePatchSuggestions(builder, SuggestionHelper.parsePatchCursor(builder.getRemaining()),
                        buildTicketFields(project));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    private static Map<String, List<String>> buildTicketFilterFields(Project project) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("title", null);
        m.put("priority", List.of("LOW", "MEDIUM", "HIGH", "URGENT"));
        if (project != null) {
            m.put("status", project.getStatuses().stream().map(s -> s.key()).toList());
            m.put("type", project.getTicketTypes().stream().map(t -> t.key()).toList());
        } else {
            m.put("status", null);
            m.put("type", null);
        }
        return m;
    }

    private static Map<String, List<String>> buildTicketFields(Project project) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("title", null);
        m.put("description", null);
        m.put("priority", List.of("LOW", "MEDIUM", "HIGH", "URGENT"));
        if (project != null) {
            m.put("status", project.getStatuses().stream().map(s -> s.key()).toList());
            m.put("type", project.getTicketTypes().stream().map(t -> t.key()).toList());
        } else {
            m.put("status", null);
            m.put("type", null);
        }
        return m;
    }
}
