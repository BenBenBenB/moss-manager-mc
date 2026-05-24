package com.mossman.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mossman.command.Mutations.Result;
import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.CreateProjectUseCase;
import com.mossman.core.usecase.project.DeleteProjectUseCase;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.core.usecase.project.RenameProjectUseCase;
import com.mossman.core.usecase.project.SetAllowNonMembersUseCase;
import com.mossman.core.usecase.project.TransferOwnershipUseCase;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.server.ServerProjects;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** {@code /mossman project ...} — list / view / create / delete / update. */
final class ProjectCommands {

    private ProjectCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> subtree() {
        return Commands.literal("project")
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("view")
                        .then(CommandArgs.projectIdArg()
                                .executes(ctx -> view(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> create(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("delete")
                        .then(CommandArgs.projectIdArg()
                                .executes(ctx -> delete(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("update")
                        .then(CommandArgs.projectIdArg()
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> update(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))));
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;

        List<Project> visible = new ProjectQueries(repo).listProjects(actor.getUUID());
        if (visible.isEmpty()) {
            Mutations.report(source, "No projects you can view.");
            return 0;
        }
        visible.stream()
                .sorted(Comparator.comparing(Project::id))
                .forEach(p -> Mutations.reportComponent(source,
                        Component.literal(p.id() + " — " + p.name() + " ")
                                .append(ChatHelpers.createRunLink("[view]",
                                        "/mossman project view " + p.id(),
                                        "View " + p.name(), ChatFormatting.GREEN))));
        return visible.size();
    }

    private static int view(CommandSourceStack source, String id) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), id).orElse(null);
            if (p == null) {
                source.sendFailure(Component.literal("No project '" + id + "'."));
                return 0;
            }
            Mutations.report(source, "== " + p.id() + " — " + p.name() + " ==");
            source.sendSuccess(() -> Component.literal("Owner: ")
                    .append(ChatHelpers.playerName(source.getServer(), p.ownerUuid())), false);
            Mutations.report(source, "Allow non-members: " + p.allowNonMembers());
            Mutations.reportComponent(source, Component.literal("Roles: " + p.roles().size() + " ")
                    .append(ChatHelpers.createRunLink("[list]",
                            "/mossman project config role list " + id, "List roles", ChatFormatting.GREEN)));
            Mutations.reportComponent(source, Component.literal("Statuses: " + p.statuses().size() + " ")
                    .append(ChatHelpers.createRunLink("[list]",
                            "/mossman project config status list " + id, "List statuses", ChatFormatting.GREEN)));
            Mutations.reportComponent(source, Component.literal("Types: " + p.types().size() + " ")
                    .append(ChatHelpers.createRunLink("[list]",
                            "/mossman project config type list " + id, "List types", ChatFormatting.GREEN)));
            if (p.tickets().isEmpty()) {
                Mutations.report(source, "Tickets: 0");
            } else {
                Mutations.reportComponent(source, Component.literal("Tickets: " + p.tickets().size() + " ")
                        .append(ChatHelpers.createRunLink("[list]",
                                "/mossman ticket list " + id, "List tickets", ChatFormatting.GREEN)));
                p.tickets().stream()
                        .sorted(Comparator.comparingInt(Ticket::number))
                        .forEach(t -> Mutations.reportComponent(source,
                                Component.literal("  #" + t.number() + " — " + t.title() + " ")
                                        .append(ChatHelpers.createRunLink("[view]",
                                                "/mossman ticket view " + id + " " + t.number(),
                                                "View ticket #" + t.number(), ChatFormatting.GREEN))));
            }
            Mutations.reportComponent(source, Mutations.buttonRow(
                    ChatHelpers.createSuggestLink("[update]",
                            "/mossman project update " + id + " {}",
                            "Update project fields", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[delete]",
                            "/mossman project delete " + id,
                            "Delete this project", ChatFormatting.RED)));
            return 1;
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int create(CommandSourceStack source, String id, String name) {
        return Mutations.mutate(source, ctx -> {
            Project template = ServerProjects.template().orElse(null);
            Project created = new CreateProjectUseCase(ctx.repo())
                    .execute(ctx.actor().getUUID(), id, name, template);
            return Result.broadcast(created, "Created project " + created.id());
        });
    }

    private static int delete(CommandSourceStack source, String id) {
        return Mutations.mutate(source, ctx -> {
            new DeleteProjectUseCase(ctx.repo()).execute(ctx.actor().getUUID(), id);
            return Result.removal(id, "Deleted project " + id);
        });
    }

    private static int update(CommandSourceStack source, String id, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            p.optionalString("name").ifPresent(name ->
                    new RenameProjectUseCase(ctx.repo()).execute(ctx.actor().getUUID(), id, name));
            p.optionalBool("allowNonMembers").ifPresent(allow ->
                    new SetAllowNonMembersUseCase(ctx.repo()).execute(ctx.actor().getUUID(), id, allow));
            p.optionalString("owner").ifPresent(ref -> {
                UUID newOwner = Resolvers.resolvePlayerRef(source.getServer(), ref);
                new TransferOwnershipUseCase(ctx.repo()).execute(ctx.actor().getUUID(), id, newOwner);
            });
            Project updated = ctx.repo().find(id).orElseThrow();
            return Result.broadcast(updated, "Updated project " + id);
        });
    }
}
