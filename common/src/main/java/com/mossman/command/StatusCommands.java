package com.mossman.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mossman.command.Mutations.Result;
import com.mossman.core.model.Project;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.core.usecase.status.CreateStatusUseCase;
import com.mossman.core.usecase.status.DeleteStatusUseCase;
import com.mossman.core.usecase.status.ReorderStatusesUseCase;
import com.mossman.core.usecase.status.UpdateStatusUseCase;
import com.mossman.persistence.JsonProjectRepository;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** {@code /mossman project config status ...}. */
final class StatusCommands {

    private StatusCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> subtree() {
        return Commands.literal("status")
                .then(Commands.literal("list")
                        .then(CommandArgs.projectIdArg()
                                .executes(ctx -> list(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.statusIdArg()
                                        .executes(ctx -> view(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "statusId"))))))
                .then(Commands.literal("create")
                        .then(CommandArgs.projectIdArg()
                                .then(Commands.argument("statusId", StringArgumentType.string())
                                        .executes(ctx -> create(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "statusId"),
                                                null))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> create(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.statusIdArg()
                                        .executes(ctx -> delete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "statusId"),
                                                null))
                                        .then(Commands.argument("replacementStatusId", StringArgumentType.string())
                                                .suggests(Suggestions.statusNames("projectId"))
                                                .executes(ctx -> delete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        StringArgumentType.getString(ctx, "replacementStatusId")))))))
                .then(Commands.literal("update")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.statusIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> update(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("order")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.statusIdArg()
                                        .then(Commands.argument("position", IntegerArgumentType.integer(1))
                                                .executes(ctx -> order(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        IntegerArgumentType.getInteger(ctx, "position")))))));
    }

    private static int list(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            for (TicketStatus s : p.statuses()) {
                Mutations.reportComponent(source, Component.empty()
                        .append(ChatHelpers.applyColor(Component.literal(s.name()), s.textColor()))
                        .append(" ")
                        .append(ChatHelpers.createRunLink("[view]",
                                "/mossman project config status view " + projectId + " " + Mutations.q(s.name()),
                                "View status " + s.name(), ChatFormatting.GREEN)));
            }
            Mutations.reportComponent(source, Mutations.buttonRow(
                    ChatHelpers.createSuggestLink("[create]",
                            "/mossman project config status create " + projectId + " ",
                            "Create a new status", ChatFormatting.YELLOW)));
            return p.statuses().size();
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int view(CommandSourceStack source, String projectId, String statusName) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            TicketStatus s = Resolvers.resolveStatus(p, statusName);
            Mutations.reportComponent(source, Component.literal("== Status: ")
                    .append(ChatHelpers.applyColor(Component.literal(s.name()), s.textColor()))
                    .append(" =="));
            Mutations.reportComponent(source, Component.literal("Text color: ")
                    .append(ChatHelpers.colorSwatch(s.textColor())));
            Mutations.reportComponent(source, Component.literal("Background:  ")
                    .append(ChatHelpers.colorSwatch(s.backgroundColor())));
            long tickets = p.tickets().stream().filter(t -> t.statusId().equals(s.id())).count();
            Mutations.report(source, "Tickets in this status: " + tickets);
            String qName = Mutations.q(s.name());
            Mutations.reportComponent(source, Mutations.buttonRow(
                    ChatHelpers.createSuggestLink("[update]",
                            "/mossman project config status update " + projectId + " " + qName + " {}",
                            "Update status fields", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[order]",
                            "/mossman project config status order " + projectId + " " + qName + " ",
                            "Reorder this status", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[delete]",
                            "/mossman project config status delete " + projectId + " " + qName,
                            "Delete this status", ChatFormatting.RED)));
            return 1;
        } catch (UseCaseException | IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int create(CommandSourceStack source, String projectId, String statusName, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            int textColor = p.optionalInt("textColor").orElse(0xFFFFFF);
            int bgColor = p.optionalInt("backgroundColor").orElse(0x000000);
            new CreateStatusUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, statusName, textColor, bgColor);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created status " + statusName);
        });
    }

    private static int delete(CommandSourceStack source, String projectId, String statusName,
                              String replacementName) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            TicketStatus toDelete = Resolvers.resolveStatus(current, statusName);
            TicketStatus replacement = replacementName == null
                    ? current.statuses().stream()
                            .filter(s -> !s.id().equals(toDelete.id()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "cannot delete the only status in '" + projectId + "'"))
                    : Resolvers.resolveStatus(current, replacementName);
            new DeleteStatusUseCase(ctx.repo())
                    .execute(ctx.actor().getUUID(), projectId, toDelete.id(), replacement.id());
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated,
                    "Deleted status " + statusName + " (tickets moved to " + replacement.name() + ")");
        });
    }

    private static int update(CommandSourceStack source, String projectId, String statusName, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            TicketStatus existing = Resolvers.resolveStatus(current, statusName);
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name").orElse(existing.name());
            int textColor = p.optionalInt("textColor").orElse(existing.textColor());
            int bgColor = p.optionalInt("backgroundColor").orElse(existing.backgroundColor());
            new UpdateStatusUseCase(ctx.repo())
                    .execute(ctx.actor().getUUID(), projectId, existing.id(), name, textColor, bgColor);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated status " + statusName);
        });
    }

    private static int order(CommandSourceStack source, String projectId, String statusName, int position) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID id = Resolvers.resolveStatus(current, statusName).id();
            List<UUID> order = Resolvers.moveToPosition(
                    current.statuses().stream().map(TicketStatus::id).collect(Collectors.toList()),
                    id, position);
            new ReorderStatusesUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, order);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved status " + statusName + " to position " + position);
        });
    }
}
