package com.mossman.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Tiny Component-builder helpers for chat output. Uses MC's native
 * {@link MutableComponent} API — no raw tellraw JSON assembly. Shape lifted
 * from the prior project's {@code TuiHelper} (Yarn mappings) and translated
 * to the Mojang mappings this project uses.
 */
public final class ChatHelpers {

    private ChatHelpers() {}

    /** Click-to-run-command link. Green by default; callers may override. */
    public static MutableComponent createRunLink(String display, String command, String hover,
                                                 ChatFormatting... formatting) {
        return createRunLink(display, command, Component.literal(hover), formatting);
    }

    public static MutableComponent createRunLink(String display, String command, Component hover,
                                                 ChatFormatting... formatting) {
        MutableComponent text = Component.literal(display).withStyle(formatting);
        return text.withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(hover)));
    }

    /** Click-to-prefill-command link. Yellow by convention; pass RED for destructive. */
    public static MutableComponent createSuggestLink(String display, String command, String hover,
                                                     ChatFormatting... formatting) {
        return createSuggestLink(display, command, Component.literal(hover), formatting);
    }

    public static MutableComponent createSuggestLink(String display, String command, Component hover,
                                                     ChatFormatting... formatting) {
        MutableComponent text = Component.literal(display).withStyle(formatting);
        return text.withStyle(style -> style
                .withClickEvent(new ClickEvent.SuggestCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(hover)));
    }

    /**
     * Applies a {@code #RRGGBB} hex color (or named {@link ChatFormatting})
     * to {@code text}. Returns {@code text} unchanged if {@code textColor} is
     * blank or unrecognised.
     */
    public static MutableComponent applyColor(MutableComponent text, String textColor) {
        if (textColor == null || textColor.isBlank()) return text;
        ChatFormatting fmt = ChatFormatting.getByName(textColor.toLowerCase());
        if (fmt != null && fmt.isColor()) return text.withStyle(s -> s.withColor(fmt));
        if (textColor.startsWith("#") && textColor.length() == 7) {
            try {
                int rgb = Integer.parseInt(textColor.substring(1), 16);
                return text.withStyle(s -> s.withColor(rgb));
            } catch (NumberFormatException ignored) {}
        }
        return text;
    }

    /**
     * Renders a player by their UUID as their in-game name with the full
     * UUID exposed in a hover tooltip. Falls back to a short UUID prefix
     * when the player isn't currently online — keep the scope small;
     * offline-aware name resolution (profile cache) can come later.
     */
    public static MutableComponent playerName(MinecraftServer server, UUID uuid) {
        String display = onlineName(server, uuid).orElseGet(() -> shortUuid(uuid));
        MutableComponent text = Component.literal(display);
        text.setStyle(text.getStyle()
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(uuid.toString()))));
        return text;
    }

    private static java.util.Optional<String> onlineName(MinecraftServer server, UUID uuid) {
        if (server == null) return java.util.Optional.empty();
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        return player == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(player.getGameProfile().name());
    }

    private static String shortUuid(UUID uuid) {
        String s = uuid.toString();
        return s.substring(0, 8) + "…";
    }
}
