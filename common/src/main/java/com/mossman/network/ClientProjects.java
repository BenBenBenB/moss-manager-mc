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
 * <p>The receiver is registered from common init via {@link Packets#register()};
 * on a dedicated server the registration is a no-op (no client receiver
 * installed) and {@link #onSyncProject} is never invoked.
 */
public final class ClientProjects {

    private static final System.Logger LOG = System.getLogger(ClientProjects.class.getName());
    private static final ProjectJson JSON = new ProjectJson();
    private static final ConcurrentMap<String, Project> CACHE = new ConcurrentHashMap<>();

    private ClientProjects() {}

    public static Optional<Project> find(String projectId) {
        return Optional.ofNullable(CACHE.get(projectId));
    }

    public static Collection<Project> all() {
        return Collections.unmodifiableCollection(CACHE.values());
    }

    public static void clear() {
        CACHE.clear();
    }

    static void onSyncProject(SyncProjectS2C packet, NetworkManager.PacketContext ctx) {
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
