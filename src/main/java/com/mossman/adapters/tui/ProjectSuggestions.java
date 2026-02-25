package com.mossman.adapters.tui;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mossman.MossManMod;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ProjectSuggestions {

    private ProjectSuggestions() {}

    static final List<String> MC_COLORS = List.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
            "dark_purple", "gold", "gray", "dark_gray", "blue",
            "green", "aqua", "red", "light_purple", "yellow", "white",
            "#RRGGBB"
    );

    static final Map<String, List<String>> PROJECT_FIELDS;
    static final Map<String, List<String>> MEMBER_FIELDS;
    static final Map<String, List<String>> STATUS_FIELDS;
    static final Map<String, List<String>> TICKET_TYPE_FIELDS;
    static final Map<String, List<String>> RELATIONSHIP_TYPE_FIELDS;

    static {
        Map<String, List<String>> m;

        m = new LinkedHashMap<>();
        m.put("name", null);
        m.put("description", null);
        m.put("ticketPrefix", null);
        m.put("iconTexture", null);
        m.put("textColor", MC_COLORS);
        m.put("externalUserPermission", List.of("FORBID", "VIEWER", "CREATOR", "EDITOR", "ADMIN"));
        PROJECT_FIELDS = Collections.unmodifiableMap(m);

        m = new LinkedHashMap<>();
        m.put("title", null);
        m.put("permission", List.of("VIEWER", "CREATOR", "EDITOR", "ADMIN"));
        MEMBER_FIELDS = Collections.unmodifiableMap(m);

        m = new LinkedHashMap<>();
        m.put("key", null);
        m.put("displayName", null);
        m.put("textColor", MC_COLORS);
        STATUS_FIELDS = Collections.unmodifiableMap(m);

        m = new LinkedHashMap<>();
        m.put("key", null);
        m.put("displayName", null);
        m.put("textColor", MC_COLORS);
        TICKET_TYPE_FIELDS = Collections.unmodifiableMap(m);

        m = new LinkedHashMap<>();
        m.put("key", null);
        m.put("displayName", null);
        m.put("textColor", MC_COLORS);
        m.put("sourceToTargetDescription", null);
        m.put("targetToSourceDescription", null);
        RELATIONSHIP_TYPE_FIELDS = Collections.unmodifiableMap(m);
    }

    /**
     * Returns a provider that suggests status names for the project identified
     * by the already-parsed {@code prefixArgName} argument in the context.
     */
    public static SuggestionProvider<ServerCommandSource> suggestStatusNames(String prefixArgName) {
        return (ctx, builder) -> {
            try {
                String prefix = StringArgumentType.getString(ctx, prefixArgName);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                return SuggestionHelper.suggest(builder, project.getStatuses().stream().map(s -> s.key()));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /**
     * Returns a provider that suggests ticket type names for the project identified
     * by the already-parsed {@code prefixArgName} argument in the context.
     */
    public static SuggestionProvider<ServerCommandSource> suggestTicketTypeNames(String prefixArgName) {
        return (ctx, builder) -> {
            try {
                String prefix = StringArgumentType.getString(ctx, prefixArgName);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                return SuggestionHelper.suggest(builder, project.getTicketTypes().stream().map(t -> t.key()));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /**
     * Returns a provider that suggests relationship type names for the project identified
     * by the already-parsed {@code prefixArgName} argument in the context.
     */
    public static SuggestionProvider<ServerCommandSource> suggestRelationshipTypeNames(String prefixArgName) {
        return (ctx, builder) -> {
            try {
                String prefix = StringArgumentType.getString(ctx, prefixArgName);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                return SuggestionHelper.suggest(builder, project.getRelationshipTypes().stream().map(r -> r.key()));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /**
     * Returns a provider that suggests relationship type names for the project that
     * owns the ticket identified by the already-parsed {@code keyArgName} argument
     * (e.g. "MOSS-1"). Useful for {@code ticket link} and {@code ticket unlink}.
     */
    public static SuggestionProvider<ServerCommandSource> suggestRelationshipTypeNamesForKey(String keyArgName) {
        return (ctx, builder) -> {
            try {
                String key = StringArgumentType.getString(ctx, keyArgName);
                String[] parts = key.split("-");
                if (parts.length < 2) return builder.buildFuture();
                String prefix = parts[0];
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                return SuggestionHelper.suggest(builder, project.getRelationshipTypes().stream().map(r -> r.key()));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /** Returns a patch suggestion provider for {@code project update}. */
    public static SuggestionProvider<ServerCommandSource> suggestProjectPatch() {
        return (ctx, builder) -> SuggestionHelper.generatePatchSuggestions(
                builder, SuggestionHelper.parsePatchCursor(builder.getRemaining()), PROJECT_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project member update}. */
    public static SuggestionProvider<ServerCommandSource> suggestMemberPatch() {
        return (ctx, builder) -> SuggestionHelper.generatePatchSuggestions(
                builder, SuggestionHelper.parsePatchCursor(builder.getRemaining()), MEMBER_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project status update}. */
    public static SuggestionProvider<ServerCommandSource> suggestStatusPatch() {
        return (ctx, builder) -> SuggestionHelper.generatePatchSuggestions(
                builder, SuggestionHelper.parsePatchCursor(builder.getRemaining()), STATUS_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project ticketType update}. */
    public static SuggestionProvider<ServerCommandSource> suggestTicketTypePatch() {
        return (ctx, builder) -> SuggestionHelper.generatePatchSuggestions(
                builder, SuggestionHelper.parsePatchCursor(builder.getRemaining()), TICKET_TYPE_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project relationshipType update}. */
    public static SuggestionProvider<ServerCommandSource> suggestRelationshipTypePatch() {
        return (ctx, builder) -> SuggestionHelper.generatePatchSuggestions(
                builder, SuggestionHelper.parsePatchCursor(builder.getRemaining()), RELATIONSHIP_TYPE_FIELDS);
    }
}
