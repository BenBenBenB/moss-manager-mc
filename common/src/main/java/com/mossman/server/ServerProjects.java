package com.mossman.server;

import com.mossman.core.model.Project;
import com.mossman.persistence.DefaultProjectTemplate;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.persistence.ProjectJson;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Server-side wiring for the project repository. On SERVER_STARTED the world
 * path is resolved, a {@link JsonProjectRepository} is built under
 * {@code <world>/mossmandata/projects/}, the default-project template is
 * loaded (or seeded) at {@code <world>/mossmandata/default.json}, and existing
 * projects are loaded into the cache. On SERVER_STOPPING the executor is
 * drained and shut down.
 */
public final class ServerProjects {

    private static volatile JsonProjectRepository current;
    private static volatile Project template;

    private ServerProjects() {}

    public static void register() {
        LifecycleEvent.SERVER_STARTED.register(ServerProjects::onStarted);
        LifecycleEvent.SERVER_STOPPING.register(ServerProjects::onStopping);
    }

    public static Optional<JsonProjectRepository> repository() {
        return Optional.ofNullable(current);
    }

    public static Optional<Project> template() {
        return Optional.ofNullable(template);
    }

    private static void onStarted(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path mossmanRoot = worldRoot.resolve("mossmandata");
        Path projectsDir = mossmanRoot.resolve("projects");
        ProjectJson json = new ProjectJson();
        JsonProjectRepository repo = new JsonProjectRepository(projectsDir, json);
        repo.loadAll();
        current = repo;
        template = DefaultProjectTemplate.load(mossmanRoot.resolve("default.json"), json);
    }

    private static void onStopping(MinecraftServer server) {
        JsonProjectRepository repo = current;
        current = null;
        template = null;
        if (repo != null) {
            repo.close();
        }
    }
}
