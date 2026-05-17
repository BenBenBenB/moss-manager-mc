package com.mossman.server;

import com.mossman.persistence.JsonProjectRepository;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Server-side wiring for the project repository. On SERVER_STARTED the world
 * path is resolved, a {@link JsonProjectRepository} is built under
 * {@code <world>/mossmandata/projects/}, and existing projects are loaded
 * into the cache. On SERVER_STOPPING the executor is drained and shut down.
 */
public final class ServerProjects {

    private static volatile JsonProjectRepository current;

    private ServerProjects() {}

    public static void register() {
        LifecycleEvent.SERVER_STARTED.register(ServerProjects::onStarted);
        LifecycleEvent.SERVER_STOPPING.register(ServerProjects::onStopping);
    }

    public static Optional<JsonProjectRepository> repository() {
        return Optional.ofNullable(current);
    }

    private static void onStarted(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path projectsDir = worldRoot.resolve("mossmandata").resolve("projects");
        JsonProjectRepository repo = new JsonProjectRepository(projectsDir);
        repo.loadAll();
        current = repo;
    }

    private static void onStopping(MinecraftServer server) {
        JsonProjectRepository repo = current;
        current = null;
        if (repo != null) {
            repo.close();
        }
    }
}
