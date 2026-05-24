package com.mossman.command;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.model.TicketType;
import com.mossman.core.permission.Permission;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.persistence.JsonProjectRepository;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lookup helpers shared by the command handlers. Resolvers do not mutate;
 * they translate user input (names, numbers, player refs) into core IDs or
 * throw a {@link IllegalArgumentException} / {@link NotFoundException} with
 * a player-facing message that the top-level handler surfaces via
 * {@code sendFailure}.
 */
final class Resolvers {

    private Resolvers() {}

    static Project requireProject(JsonProjectRepository repo, String projectId) {
        return repo.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
    }

    static Role resolveRole(Project project, String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        return project.roles().stream()
                .filter(r -> r.name().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no role named '" + name + "' in project '" + project.id() + "'"));
    }

    static TicketStatus resolveStatus(Project project, String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        return project.statuses().stream()
                .filter(s -> s.name().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no status named '" + name + "' in project '" + project.id() + "'"));
    }

    static TicketType resolveType(Project project, String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        return project.types().stream()
                .filter(t -> t.name().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no type named '" + name + "' in project '" + project.id() + "'"));
    }

    static Ticket resolveTicket(Project project, int number) {
        return project.tickets().stream()
                .filter(t -> t.number() == number)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no ticket #" + number + " in project '" + project.id() + "'"));
    }

    static Permission parsePermission(String name) {
        try {
            return Permission.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown permission '" + name + "'");
        }
    }

    /**
     * Accepts either a UUID string or the name of a currently-online player.
     * Online-only fallback for now; offline-by-name needs profile-cache work.
     */
    static UUID resolvePlayerRef(MinecraftServer server, String ref) {
        try {
            return UUID.fromString(ref);
        } catch (IllegalArgumentException ignored) {
            // not a UUID — try name
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getGameProfile().name().equalsIgnoreCase(ref)) return p.getUUID();
        }
        throw new IllegalArgumentException("no online player named '" + ref
                + "' (use the player's UUID for offline targets)");
    }

    /**
     * Returns {@code currentOrder} with {@code targetId} moved to the 1-based
     * {@code position}. Position is clamped to {@code [1, size]}.
     */
    static List<UUID> moveToPosition(List<UUID> currentOrder, UUID targetId, int position) {
        List<UUID> next = new ArrayList<>(currentOrder);
        if (!next.remove(targetId)) {
            throw new IllegalArgumentException("internal: target id missing from order list");
        }
        int clamped = Math.max(1, Math.min(position, next.size() + 1));
        next.add(clamped - 1, targetId);
        // Sanity check: no duplicates (use LinkedHashSet to preserve order).
        return new ArrayList<>(new LinkedHashSet<>(next));
    }

    static UUID firstStatusId(Project project) {
        List<TicketStatus> statuses = project.statuses();
        if (statuses.isEmpty()) throw new SnbtPatch.Format("project has no statuses");
        return statuses.get(0).id();
    }

    static UUID firstTypeId(Project project) {
        List<TicketType> types = project.types();
        if (types.isEmpty()) throw new SnbtPatch.Format("project has no types");
        return types.get(0).id();
    }
}
