package com.mossman.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mossman.core.model.Project;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.CreateProjectUseCase;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.network.ServerProjectSync;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.server.ServerProjects;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

/**
 * Smoke-test entry point for the project pipeline: lets a player drive
 * {@link CreateProjectUseCase} and {@link ProjectQueries} from chat instead of
 * the (not-yet-built) UI. Sits next to the C2S handler; both call the same
 * use cases and the same broadcast helper, so observable state stays
 * identical regardless of which entry point ran.
 */
public final class MossmanCommand {

    private MossmanCommand() {}

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) ->
                dispatcher.register(Commands.literal("mossman")
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> runCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("list")
                                .executes(ctx -> runList(ctx.getSource())))));
    }

    private static int runCreate(CommandSourceStack source, String id, String name) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;

        try {
            Project created = new CreateProjectUseCase(repo).execute(actor.getUUID(), id, name);
            source.sendSuccess(() -> Component.literal("Created project " + created.id()), false);
            ServerProjectSync.broadcastProject(
                    source.getServer().getPlayerList().getPlayers(), created);
            return 1;
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runList(CommandSourceStack source) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;

        List<Project> visible = new ProjectQueries(repo).listProjects(actor.getUUID());
        if (visible.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No projects you can view."), false);
            return 0;
        }
        visible.stream()
                .sorted(Comparator.comparing(Project::id))
                .forEach(p -> source.sendSuccess(
                        () -> Component.literal(p.id() + " — " + p.name()), false));
        return visible.size();
    }

    private static int notPlayer(CommandSourceStack source) {
        source.sendFailure(Component.literal("Only players can use /mossman."));
        return 0;
    }

    private static JsonProjectRepository requireRepo(CommandSourceStack source) {
        JsonProjectRepository repo = ServerProjects.repository().orElse(null);
        if (repo == null) {
            source.sendFailure(Component.literal("Project storage is not ready."));
        }
        return repo;
    }
}
