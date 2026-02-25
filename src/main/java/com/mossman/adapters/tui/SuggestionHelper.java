package com.mossman.adapters.tui;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mossman.MossManMod;
import com.mossman.domain.auth.PermissionChecker;
import net.minecraft.server.command.ServerCommandSource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class SuggestionHelper {

    private SuggestionHelper() {}

    /** Suggests ticket prefixes for all projects the caller can view. */
    public static CompletableFuture<Suggestions> suggestVisiblePrefixes(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        var prefixes = MossManMod.getProjectRepository()
                .findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> PermissionChecker.canView(p, ctx.getSource()))
                .map(p -> p.getTicketPrefix());
        return suggest(builder, prefixes);
    }

    /** Suggests the assignable permission values (excludes FORBID and OWNER). */
    public static CompletableFuture<Suggestions> suggestAssignablePermissions(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        return suggest(builder, List.of("VIEWER", "CREATOR", "EDITOR", "ADMIN").stream());
    }

    /**
     * Suggests the player's own mail message IDs (most recent first, up to 20),
     * with the sender name and subject shown as a tooltip.
     */
    public static CompletableFuture<Suggestions> suggestInboxMessageIds(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        var player = ctx.getSource().getPlayer();
        if (player == null) return builder.buildFuture();
        var repo = MossManMod.getMailRepository();
        if (repo == null) return builder.buildFuture();
        String remaining = builder.getRemaining();
        for (var msg : repo.findByRecipientId(player.getUuid(), 0, 20)) {
            String id = String.valueOf(msg.id());
            if (id.startsWith(remaining)) {
                builder.suggest(id, net.minecraft.text.Text.literal(msg.senderName() + ": " + msg.subject()));
            }
        }
        return builder.buildFuture();
    }

    /** Curated list of commonly used IANA timezone IDs. */
    static final List<String> COMMON_TIMEZONES = List.of(
            "UTC",
            "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
            "America/Anchorage", "America/Honolulu",
            "America/Toronto", "America/Vancouver", "America/Sao_Paulo", "America/Argentina/Buenos_Aires",
            "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Madrid",
            "Europe/Amsterdam", "Europe/Rome", "Europe/Helsinki", "Europe/Moscow",
            "Africa/Cairo", "Africa/Nairobi", "Africa/Johannesburg",
            "Asia/Dubai", "Asia/Kolkata", "Asia/Bangkok", "Asia/Shanghai",
            "Asia/Tokyo", "Asia/Seoul", "Asia/Singapore",
            "Australia/Sydney", "Australia/Perth",
            "Pacific/Auckland", "Pacific/Honolulu"
    );

    /** Suggests a curated list of common IANA timezone IDs. */
    public static CompletableFuture<Suggestions> suggestCommonTimezones(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        return suggest(builder, COMMON_TIMEZONES.stream());
    }

    /** Filters {@code candidates} by the remaining input and adds matching entries to the builder. */
    static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Stream<String> candidates) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        candidates
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    // ==================== SNBT Patch Autocomplete ====================

    enum PatchParseState { BEFORE_OPEN, IN_KEY, AFTER_COLON, IN_VALUE, AFTER_VALUE, DONE }

    record PatchCursor(
            String completedPrefix,
            String currentToken,
            PatchParseState state,
            String currentKey,
            Set<String> seenKeys) {}

    /**
     * Walks the partial SNBT string character-by-character to determine the
     * autocomplete cursor state (what field/value position the caret is at).
     */
    static PatchCursor parsePatchCursor(String input) {
        if (input.isEmpty()) {
            return new PatchCursor("", "", PatchParseState.BEFORE_OPEN, null, new LinkedHashSet<>());
        }
        PatchParseState state = PatchParseState.BEFORE_OPEN;
        StringBuilder prefix = new StringBuilder();
        StringBuilder token = new StringBuilder();
        String currentKey = null;
        Set<String> seenKeys = new LinkedHashSet<>();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (state) {
                case BEFORE_OPEN -> {
                    if (c == '{') { prefix.append(c); state = PatchParseState.IN_KEY; }
                }
                case IN_KEY -> {
                    if (c == ':') {
                        currentKey = token.toString();
                        seenKeys.add(currentKey);
                        prefix.append(token).append(':');
                        token.setLength(0);
                        state = PatchParseState.AFTER_COLON;
                    } else if (c == ',') {
                        prefix.append(token).append(',');
                        token.setLength(0);
                    } else if (c == '}') {
                        state = PatchParseState.DONE;
                    } else {
                        token.append(c);
                    }
                }
                case AFTER_COLON -> {
                    if (c == '"') { prefix.append('"'); state = PatchParseState.IN_VALUE; }
                }
                case IN_VALUE -> {
                    if (c == '"' && (i == 0 || input.charAt(i - 1) != '\\')) {
                        prefix.append(token).append('"');
                        token.setLength(0);
                        state = PatchParseState.AFTER_VALUE;
                    } else {
                        token.append(c);
                    }
                }
                case AFTER_VALUE -> {
                    if (c == ',') { prefix.append(','); state = PatchParseState.IN_KEY; }
                    else if (c == '}') { state = PatchParseState.DONE; }
                }
                default -> {}
            }
        }
        return new PatchCursor(prefix.toString(), token.toString(), state, currentKey, seenKeys);
    }

    static CompletableFuture<Suggestions> generatePatchSuggestions(
            SuggestionsBuilder builder, PatchCursor cursor, Map<String, List<String>> fields) {
        SuggestionsBuilder ob = builder.createOffset(builder.getStart() + cursor.completedPrefix().length());
        return switch (cursor.state()) {
            case BEFORE_OPEN -> suggest(ob, Stream.of("{"));
            case IN_KEY -> suggest(ob, fields.keySet().stream()
                    .filter(f -> !cursor.seenKeys().contains(f))
                    .map(f -> f + ":"));
            case AFTER_COLON -> suggest(ob, Stream.of("\""));
            case IN_VALUE -> {
                List<String> values = cursor.currentKey() != null ? fields.get(cursor.currentKey()) : null;
                if (values != null) yield suggest(ob, values.stream().map(v -> v + "\""));
                yield suggest(ob, Stream.of("\""));
            }
            case AFTER_VALUE -> suggest(ob, Stream.of("}", ","));
            default -> ob.buildFuture();
        };
    }
}
