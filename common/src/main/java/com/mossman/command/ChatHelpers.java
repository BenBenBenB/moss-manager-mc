package com.mossman.command;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Tiny Component-builder helpers for chat output. Uses MC's native
 * {@link MutableComponent}/{@link HoverEvent} API — no raw tellraw JSON
 * assembly. Modelled on the prior project's TuiHelper but trimmed to
 * exactly what the current command surface needs.
 */
public final class ChatHelpers {

    private ChatHelpers() {}

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
