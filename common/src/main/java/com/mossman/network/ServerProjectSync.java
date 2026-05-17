package com.mossman.network;

import com.mossman.core.model.Project;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.CreateProjectUseCase;
import com.mossman.network.payload.CreateProjectC2S;
import com.mossman.network.payload.RemoveProjectS2C;
import com.mossman.network.payload.SyncProjectS2C;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.persistence.ProjectJson;
import com.mossman.server.ServerProjects;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-side glue: turns C2S packets into use-case calls, runs the join-time
 * snapshot, and broadcasts project deltas to players who can see them.
 *
 * <p>Re-uses {@link ProjectJson} for the on-wire encoding so the wire format
 * matches the on-disk format exactly.
 */
public final class ServerProjectSync {

    private static final System.Logger LOG = System.getLogger(ServerProjectSync.class.getName());
    private static final ProjectJson JSON = new ProjectJson();

    private ServerProjectSync() {}

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(ServerProjectSync::onPlayerJoin);
    }

    static void handleCreateProject(CreateProjectC2S packet, NetworkManager.PacketContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
        ctx.queue(() -> {
            JsonProjectRepository repo = ServerProjects.repository().orElse(null);
            if (repo == null) {
                LOG.log(System.Logger.Level.WARNING,
                        "received CreateProjectC2S from {0} but no repository is initialized",
                        player.getUUID());
                return;
            }
            try {
                Project created = new CreateProjectUseCase(repo)
                        .execute(player.getUUID(), packet.id(), packet.name());
                broadcastProject(player.level().getServer().getPlayerList().getPlayers(), created);
            } catch (UseCaseException e) {
                reportError(player, e);
            } catch (Exception e) {
                LOG.log(System.Logger.Level.ERROR, "create project failed", e);
                player.sendSystemMessage(Component.literal("Could not create project: " + e.getMessage()));
            }
        });
    }

    private static void onPlayerJoin(ServerPlayer player) {
        JsonProjectRepository repo = ServerProjects.repository().orElse(null);
        if (repo == null) return;
        UUID actor = player.getUUID();
        for (Project p : repo.list()) {
            if (PermissionEvaluator.has(p, actor, Permission.VIEW_PROJECT)) {
                sendTo(player, p);
            }
        }
    }

    /** Sends {@code project} to every player who currently has VIEW_PROJECT on it. */
    public static void broadcastProject(Iterable<ServerPlayer> players, Project project) {
        SyncProjectS2C payload = new SyncProjectS2C(JSON.toJson(project));
        for (ServerPlayer p : players) {
            if (PermissionEvaluator.has(project, p.getUUID(), Permission.VIEW_PROJECT)) {
                NetworkManager.sendToPlayer(p, payload);
            }
        }
    }

    /** Tells every online player to drop {@code projectId} from their cache. */
    public static void broadcastRemoval(Iterable<ServerPlayer> players, String projectId) {
        RemoveProjectS2C payload = new RemoveProjectS2C(projectId);
        for (ServerPlayer p : players) {
            NetworkManager.sendToPlayer(p, payload);
        }
    }

    private static void sendTo(ServerPlayer player, Project project) {
        NetworkManager.sendToPlayer(player, new SyncProjectS2C(JSON.toJson(project)));
    }

    private static void reportError(ServerPlayer player, UseCaseException e) {
        player.sendSystemMessage(Component.literal(e.getMessage()));
    }
}
