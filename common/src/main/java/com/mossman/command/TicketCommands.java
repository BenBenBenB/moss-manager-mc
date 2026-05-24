package com.mossman.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mossman.command.Mutations.Result;
import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.model.TicketType;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.core.usecase.ticket.AssignTicketUseCase;
import com.mossman.core.usecase.ticket.ChangeTicketStatusUseCase;
import com.mossman.core.usecase.ticket.ChangeTicketTypeUseCase;
import com.mossman.core.usecase.ticket.CreateTicketUseCase;
import com.mossman.core.usecase.ticket.DeleteTicketUseCase;
import com.mossman.core.usecase.ticket.UpdateTicketUseCase;
import com.mossman.persistence.JsonProjectRepository;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.UUID;

/**
 * {@code /mossman ticket ...} — list / view / create / delete / update.
 *
 * <p>Ticket update patch keys: title, description, status (name), type
 * (name), assignee (UUID or online player name).
 */
final class TicketCommands {

    private TicketCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> subtree() {
        return Commands.literal("ticket")
                .then(Commands.literal("list")
                        .then(CommandArgs.projectIdArg()
                                .executes(ctx -> list(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.ticketIdArg()
                                        .executes(ctx -> view(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                IntegerArgumentType.getInteger(ctx, "ticketId"))))))
                .then(Commands.literal("create")
                        .then(CommandArgs.projectIdArg()
                                .executes(ctx -> create(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId"),
                                        null))
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> create(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("delete")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.ticketIdArg()
                                        .executes(ctx -> delete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                IntegerArgumentType.getInteger(ctx, "ticketId"))))))
                .then(Commands.literal("update")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.ticketIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> update(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        IntegerArgumentType.getInteger(ctx, "ticketId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))));
    }

    private static int list(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            if (p.tickets().isEmpty()) {
                Mutations.report(source, "No tickets in " + projectId + ".");
                return 0;
            }
            p.tickets().stream()
                    .sorted(Comparator.comparingInt(Ticket::number))
                    .forEach(t -> Mutations.reportComponent(source,
                            Component.literal("#" + t.number() + " — " + t.title() + " ")
                                    .append(ChatHelpers.createRunLink("[view]",
                                            "/mossman ticket view " + projectId + " " + t.number(),
                                            "View ticket #" + t.number(), ChatFormatting.GREEN))));
            return p.tickets().size();
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int view(CommandSourceStack source, String projectId, int number) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            Ticket t = Resolvers.resolveTicket(p, number);
            TicketStatus s = p.findStatus(t.statusId()).orElse(null);
            TicketType ty = p.findType(t.typeId()).orElse(null);
            Mutations.report(source, "== Ticket #" + t.number() + " — " + t.title() + " ==");
            Mutations.report(source, "Status: " + (s == null ? "(missing)" : s.name()));
            Mutations.report(source, "Type:   " + (ty == null ? "(missing)" : ty.name()));
            if (t.assigneeUuid() == null) {
                Mutations.report(source, "Assignee: (unassigned)");
            } else {
                final UUID a = t.assigneeUuid();
                source.sendSuccess(() -> Component.literal("Assignee: ")
                        .append(ChatHelpers.playerName(source.getServer(), a)), false);
            }
            if (!t.description().isBlank()) {
                Mutations.report(source, "Description: " + t.description());
            }
            Mutations.reportComponent(source, Mutations.buttonRow(
                    ChatHelpers.createSuggestLink("[update]",
                            "/mossman ticket update " + projectId + " " + t.number() + " {}",
                            "Update ticket fields", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[assign]",
                            "/mossman ticket update " + projectId + " " + t.number() + " {assignee:\"\"}",
                            "Assign this ticket", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[delete]",
                            "/mossman ticket delete " + projectId + " " + t.number(),
                            "Delete this ticket", ChatFormatting.RED)));
            return 1;
        } catch (UseCaseException | IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int create(CommandSourceStack source, String projectId, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            String title = p.optionalString("title")
                    .orElseThrow(() -> new SnbtPatch.Format("ticket create requires 'title'"));
            String description = p.optionalString("description").orElse("");
            UUID assignee = p.optionalString("assignee")
                    .map(ref -> Resolvers.resolvePlayerRef(source.getServer(), ref))
                    .orElse(null);
            UUID statusId = p.optionalString("status")
                    .map(name -> Resolvers.resolveStatus(current, name).id())
                    .orElseGet(() -> Resolvers.firstStatusId(current));
            UUID typeId = p.optionalString("type")
                    .map(name -> Resolvers.resolveType(current, name).id())
                    .orElseGet(() -> Resolvers.firstTypeId(current));
            Ticket created = new CreateTicketUseCase(ctx.repo())
                    .execute(ctx.actor().getUUID(), projectId, title, description, assignee, statusId, typeId);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created ticket #" + created.number());
        });
    }

    private static int delete(CommandSourceStack source, String projectId, int number) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID ticketId = Resolvers.resolveTicket(current, number).id();
            new DeleteTicketUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, ticketId);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted ticket #" + number);
        });
    }

    private static int update(CommandSourceStack source, String projectId, int number, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID ticketId = Resolvers.resolveTicket(current, number).id();
            SnbtPatch p = new SnbtPatch(patch);

            if (p.has("title") || p.has("description")) {
                String newTitle = p.optionalString("title").orElse(null);
                String newDescription = p.optionalString("description").orElse(null);
                new UpdateTicketUseCase(ctx.repo())
                        .execute(ctx.actor().getUUID(), projectId, ticketId, newTitle, newDescription);
            }
            p.optionalString("status").ifPresent(name -> {
                UUID statusId = Resolvers.resolveStatus(current, name).id();
                new ChangeTicketStatusUseCase(ctx.repo())
                        .execute(ctx.actor().getUUID(), projectId, ticketId, statusId);
            });
            p.optionalString("type").ifPresent(name -> {
                UUID typeId = Resolvers.resolveType(current, name).id();
                new ChangeTicketTypeUseCase(ctx.repo())
                        .execute(ctx.actor().getUUID(), projectId, ticketId, typeId);
            });
            if (p.has("assignee")) {
                UUID assignee = p.optionalString("assignee")
                        .map(ref -> ref.isEmpty() ? null : Resolvers.resolvePlayerRef(source.getServer(), ref))
                        .orElse(null);
                new AssignTicketUseCase(ctx.repo())
                        .execute(ctx.actor().getUUID(), projectId, ticketId, assignee);
            }
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated ticket #" + number);
        });
    }
}
