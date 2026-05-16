package com.mossman.network;

import com.mossman.core.model.Project;
import com.mossman.network.payload.SyncProjectS2C;
import com.mossman.persistence.ProjectJson;

import dev.architectury.networking.NetworkManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Client-side read-only cache of projects pushed from the server. The UI will
 * read from this; mutations go back over the wire via C2S packets.
 *
 * <p>Loaded class-side only — never touch this from the dedicated server.
 */
public final class ClientProjects {

    private static final System.Logger LOG = System.getLogger(ClientProjects.class.getName());
    private static final ProjectJson JSON = new ProjectJson();
    private static final ConcurrentMap<String, Project> CACHE = new ConcurrentHashMap<>();

    private ClientProjects() {}

    public static void register() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                SyncProjectS2C.TYPE,
                SyncProjectS2C.CODEC,
                ClientProjects::onSyncProject);
    }

    public static Optional<Project> find(String projectId) {
        return Optional.ofNullable(CACHE.get(projectId));
    }

    public static Collection<Project> all() {
        return Collections.unmodifiableCollection(CACHE.values());
    }

    public static void clear() {
        CACHE.clear();
    }

    private static void onSyncProject(SyncProjectS2C packet, NetworkManager.PacketContext ctx) {
        Project project;
        try {
            project = JSON.fromJson(packet.projectJson());
        } catch (Exception e) {
            LOG.log(System.Logger.Level.ERROR, "failed to decode incoming project sync", e);
            return;
        }
        if (project == null) return;
        ctx.queue(() -> CACHE.put(project.id(), project));
    }
}
