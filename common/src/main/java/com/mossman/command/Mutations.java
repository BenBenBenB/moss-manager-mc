package com.mossman.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mossman.core.model.Project;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.network.ServerProjectSync;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.server.ServerProjects;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Scaffolding shared by every mutating /mossman handler: the player+repo
 * context record, the success/removal {@link Result} discriminator, and the
 * {@link #mutate} method that runs the body, broadcasts the resulting
 * Project state, and converts use-case exceptions into player-facing
 * failure messages. Also holds the small output helpers ({@link #report},
 * {@link #buttonRow}, etc.) since they're used both inside {@code mutate}
 * and by the read-only view handlers.
 */
final class Mutations {

    private Mutations() {}

    record MutationCtx(ServerPlayer actor, JsonProjectRepository repo) {}

    sealed interface Result permits Result.Broadcast, Result.Removal {
        static Result broadcast(Project project, String message) { return new Broadcast(project, message); }
        static Result removal(String projectId, String message) { return new Removal(projectId, message); }
        record Broadcast(Project project, String message) implements Result {}
        record Removal(String projectId, String message) implements Result {}
    }

    @FunctionalInterface
    interface MutationBody {
        Result run(MutationCtx ctx) throws CommandSyntaxException;
    }

    static int mutate(CommandSourceStack source, MutationBody body) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Result result = body.run(new MutationCtx(actor, repo));
            Iterable<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
            switch (result) {
                case Result.Broadcast b -> ServerProjectSync.broadcastProject(players, b.project());
                case Result.Removal r -> ServerProjectSync.broadcastRemoval(players, r.projectId());
            }
            report(source, switch (result) {
                case Result.Broadcast b -> b.message();
                case Result.Removal r -> r.message();
            });
            return 1;
        } catch (UseCaseException | IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    // ===== output helpers ==============================================

    static void report(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    static void reportComponent(CommandSourceStack source, Component component) {
        source.sendSuccess(() -> component, false);
    }

    /** Quote {@code s} if it contains a space or quote so it parses back as one StringArgument. */
    static String q(String s) {
        if (s.indexOf(' ') < 0 && s.indexOf('"') < 0) return s;
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    static MutableComponent buttonRow(Component... buttons) {
        MutableComponent row = Component.literal("  ");
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) row.append(" ");
            row.append(buttons[i]);
        }
        return row;
    }

    static int notPlayer(CommandSourceStack source) {
        source.sendFailure(Component.literal("Only players can use /mossman."));
        return 0;
    }

    static JsonProjectRepository requireRepo(CommandSourceStack source) {
        JsonProjectRepository repo = ServerProjects.repository().orElse(null);
        if (repo == null) {
            source.sendFailure(Component.literal("Project storage is not ready."));
        }
        return repo;
    }
}
