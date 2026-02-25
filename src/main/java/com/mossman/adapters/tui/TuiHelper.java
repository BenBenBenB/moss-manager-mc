package com.mossman.adapters.tui;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TuiHelper {

    /**
     * Creates a clickable text component that runs a command.
     */
    public static MutableText createRunLink(String display, String command, String hoverText, Formatting... formatting) {
        return createRunLink(display, command, Text.literal(hoverText), formatting);
    }

    public static MutableText createRunLink(String display, String command, Text hoverText, Formatting... formatting) {
        MutableText text = Text.literal(display).formatted(formatting);
        text.styled(style -> style
                .withClickEvent(new net.minecraft.text.ClickEvent.RunCommand(command))
                .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(hoverText)));
        return text;
    }

    /**
     * Creates a clickable text component that suggests a command.
     */
    public static MutableText createSuggestLink(String display, String command, String hoverText, Formatting... formatting) {
        return createSuggestLink(display, command, Text.literal(hoverText), formatting);
    }

    public static MutableText createSuggestLink(String display, String command, Text hoverText, Formatting... formatting) {
        MutableText text = Text.literal(display).formatted(formatting);
        text.styled(style -> style
                .withClickEvent(new net.minecraft.text.ClickEvent.SuggestCommand(command))
                .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(hoverText)));
        return text;
    }

    /**
     * Creates a standard translatable component.
     */
    public static MutableText translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }

    /**
     * Applies a textColor string (named Minecraft color or {@code #RRGGBB} hex) to
     * {@code text} by overriding its color style. Returns {@code text} unchanged if
     * {@code textColor} is null, blank, or unrecognised.
     */
    public static MutableText applyColor(MutableText text, String textColor) {
        if (textColor == null || textColor.isBlank()) return text;
        Formatting fmt = Formatting.byName(textColor.toLowerCase());
        if (fmt != null && fmt.isColor()) return text.styled(s -> s.withColor(fmt));
        if (textColor.startsWith("#") && textColor.length() == 7) {
            try {
                int rgb = Integer.parseInt(textColor.substring(1), 16);
                return text.styled(s -> s.withColor(rgb));
            } catch (NumberFormatException ignored) {}
        }
        return text;
    }

    /**
     * Resolves the configured timezone for the player issuing {@code source}.
     * Falls back to UTC if no setting exists or the stored zone ID is invalid.
     */
    public static ZoneId resolveZone(ServerCommandSource source) {
        if (source.getPlayer() == null) return ZoneOffset.UTC;
        UUID playerId = source.getPlayer().getUuid();
        var repo = com.mossman.MossManMod.getPlayerSettingsRepository();
        if (repo == null) return ZoneOffset.UTC;
        return repo.findByPlayerId(playerId)
                .map(s -> {
                    try { return ZoneId.of(s.timezone()); }
                    catch (Exception e) { return (ZoneId) ZoneOffset.UTC; }
                })
                .orElse(ZoneOffset.UTC);
    }

    /**
     * Formats an epoch-millisecond timestamp as {@code MM/dd HH:mm} in the given zone,
     * e.g. {@code "02/24 14:30"}.
     */
    public static String formatTimestamp(long millis, ZoneId zone) {
        return DateTimeFormatter.ofPattern("MM/dd HH:mm")
                .format(Instant.ofEpochMilli(millis).atZone(zone));
    }

    /**
     * Returns a red error Text for display to the player.
     * <p>
     * {@link IllegalArgumentException} and {@link SecurityException} are considered user-facing
     * validation/logic errors — their message is shown directly.
     * All other exceptions are considered internal errors and a generic message is shown instead,
     * to avoid leaking implementation details.
     */
    public static MutableText errorText(Exception e) {
        if (e instanceof IllegalArgumentException || e instanceof SecurityException) {
            return Text.literal(e.getMessage()).formatted(Formatting.RED);
        }
        return Text.literal("An unexpected error occurred.").formatted(Formatting.RED);
    }

    /**
     * Extracts the optional "page" integer argument; returns 1 if absent.
     */
    public static int getOptionalPage(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        try { return com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "page"); }
        catch (IllegalArgumentException e) { return 1; }
    }

    /**
     * Sends a « Page N of M » navigation line to the source.
     * baseCmd must be the full command without a trailing page number
     * (e.g. "/mossman project list"). Does nothing if totalPages <= 1.
     */
    public static void sendPaginationFooter(ServerCommandSource source,
            int page, int totalPages, String baseCmd) {
        if (totalPages <= 1) return;
        MutableText nav = Text.empty();
        if (page > 1)
            nav.append(createRunLink(translatable("mossman.tui.common.pagination.prev").getString(),
                    baseCmd + " " + (page - 1), "Previous Page", Formatting.GOLD)).append(" ");
        nav.append(translatable("mossman.tui.common.pagination.page_info", page, totalPages)
                .formatted(Formatting.GRAY));
        if (page < totalPages)
            nav.append(" ").append(createRunLink(translatable("mossman.tui.common.pagination.next").getString(),
                    baseCmd + " " + (page + 1), "Next Page", Formatting.GOLD));
        source.sendMessage(nav);
    }
}
