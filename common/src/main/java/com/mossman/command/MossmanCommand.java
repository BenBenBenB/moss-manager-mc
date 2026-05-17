package com.mossman.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.model.TicketType;
import com.mossman.core.permission.Permission;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.CreateProjectUseCase;
import com.mossman.core.usecase.project.DeleteProjectUseCase;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.core.usecase.project.RenameProjectUseCase;
import com.mossman.core.usecase.project.SetAllowNonMembersUseCase;
import com.mossman.core.usecase.project.TransferOwnershipUseCase;
import com.mossman.core.usecase.role.AssignRoleUseCase;
import com.mossman.core.usecase.role.CreateRoleUseCase;
import com.mossman.core.usecase.role.DeleteRoleUseCase;
import com.mossman.core.usecase.role.RenameRoleUseCase;
import com.mossman.core.usecase.role.ReorderRolesUseCase;
import com.mossman.core.usecase.role.UnassignRoleUseCase;
import com.mossman.core.usecase.role.UpdateRoleColorUseCase;
import com.mossman.core.usecase.role.UpdateRolePermissionsUseCase;
import com.mossman.core.usecase.status.CreateStatusUseCase;
import com.mossman.core.usecase.status.DeleteStatusUseCase;
import com.mossman.core.usecase.status.ReorderStatusesUseCase;
import com.mossman.core.usecase.status.UpdateStatusUseCase;
import com.mossman.core.usecase.ticket.AssignTicketUseCase;
import com.mossman.core.usecase.ticket.ChangeTicketStatusUseCase;
import com.mossman.core.usecase.ticket.ChangeTicketTypeUseCase;
import com.mossman.core.usecase.ticket.CreateTicketUseCase;
import com.mossman.core.usecase.ticket.DeleteTicketUseCase;
import com.mossman.core.usecase.ticket.UpdateTicketUseCase;
import com.mossman.core.usecase.type.CreateTypeUseCase;
import com.mossman.core.usecase.type.DeleteTypeUseCase;
import com.mossman.core.usecase.type.ReorderTypesUseCase;
import com.mossman.core.usecase.type.UpdateTypeUseCase;
import com.mossman.network.ServerProjectSync;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.server.ServerProjects;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Chat surface for every core use case. Same code path as the C2S handler:
 * both call the same use cases against {@link ServerProjects}'s repo and
 * broadcast through {@link ServerProjectSync}, so observable client state
 * is identical regardless of entry point.
 *
 * <p>Update-style and create-style leaves take an SNBT compound. Updates
 * use patch semantics — only keys present in the compound change; missing
 * keys keep the current value. Distinct semantic operations
 * (assign/unassign/permissions/reorder/delete) get their own leaves rather
 * than being folded into an SNBT shape.
 */
public final class MossmanCommand {

    private MossmanCommand() {}

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) ->
                dispatcher.register(Commands.literal("mossman")
                        .then(createLeaf())
                        .then(listLeaf())
                        .then(deleteLeaf())
                        .then(renameLeaf())
                        .then(ownerLeaf())
                        .then(nonmembersLeaf())
                        .then(showLeaf())
                        .then(roleSubtree())
                        .then(statusSubtree())
                        .then(typeSubtree())
                        .then(ticketSubtree())));
    }

    // ===== project-level leaves =========================================

    private static LiteralArgumentBuilder<CommandSourceStack> createLeaf() {
        return Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> runCreate(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "name")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listLeaf() {
        return Commands.literal("list").executes(ctx -> runList(ctx.getSource()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> deleteLeaf() {
        return Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> runDelete(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> renameLeaf() {
        return Commands.literal("rename")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("newName", StringArgumentType.greedyString())
                                .executes(ctx -> runRename(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "newName")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ownerLeaf() {
        return Commands.literal("owner")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> runOwner(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        EntityArgument.getPlayer(ctx, "player")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nonmembersLeaf() {
        return Commands.literal("nonmembers")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                .executes(ctx -> runNonmembers(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        BoolArgumentType.getBool(ctx, "allow")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> showLeaf() {
        return Commands.literal("show")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> runShow(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"))));
    }

    // ===== role subtree =================================================
    // SNBT shapes:
    //   create: {name:"Reviewer", color:0xFFAA00, permissions:["VIEW_PROJECT", ...]}
    //   update: any subset of {name, color}  (patch)
    //   permissions: {permissions:[...]} — full Set replacement
    private static LiteralArgumentBuilder<CommandSourceStack> roleSubtree() {
        return Commands.literal("role")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runRoleCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runRoleUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "roleId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .executes(ctx -> runRoleDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                UuidArgument.getUuid(ctx, "roleId"))))))
                .then(Commands.literal("permissions")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runRolePermissions(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "roleId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("assign")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runRoleAssign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "roleId"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("unassign")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runRoleUnassign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "roleId"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("reorder")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("orderedIds", StringArgumentType.greedyString())
                                        .executes(ctx -> runRoleReorder(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "orderedIds"))))));
    }

    // ===== status subtree ===============================================
    // SNBT shapes:
    //   create: {name:"In Review", textColor:0xFFFFFF, backgroundColor:0x6B21A8}
    //   update: any subset of those keys (patch)
    private static LiteralArgumentBuilder<CommandSourceStack> statusSubtree() {
        return Commands.literal("status")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runStatusCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("statusId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runStatusUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "statusId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("statusId", UuidArgument.uuid())
                                        .then(Commands.argument("replacementId", UuidArgument.uuid())
                                                .executes(ctx -> runStatusDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "statusId"),
                                                        UuidArgument.getUuid(ctx, "replacementId")))))))
                .then(Commands.literal("reorder")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("orderedIds", StringArgumentType.greedyString())
                                        .executes(ctx -> runStatusReorder(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "orderedIds"))))));
    }

    // ===== type subtree (parallel to status) ============================
    private static LiteralArgumentBuilder<CommandSourceStack> typeSubtree() {
        return Commands.literal("type")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runTypeCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("typeId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runTypeUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "typeId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("typeId", UuidArgument.uuid())
                                        .then(Commands.argument("replacementId", UuidArgument.uuid())
                                                .executes(ctx -> runTypeDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "typeId"),
                                                        UuidArgument.getUuid(ctx, "replacementId")))))))
                .then(Commands.literal("reorder")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("orderedIds", StringArgumentType.greedyString())
                                        .executes(ctx -> runTypeReorder(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "orderedIds"))))));
    }

    // ===== ticket subtree ===============================================
    // SNBT shapes:
    //   create: {title, description?, assignee?, statusId?, typeId?}
    //   update: any subset of {title, description}  (already patch-style in the use case)
    private static LiteralArgumentBuilder<CommandSourceStack> ticketSubtree() {
        return Commands.literal("ticket")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runTicketCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runTicketUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "ticketId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .executes(ctx -> runTicketDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                UuidArgument.getUuid(ctx, "ticketId"))))))
                .then(Commands.literal("assign")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runTicketAssign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "ticketId"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("status")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("statusId", UuidArgument.uuid())
                                                .executes(ctx -> runTicketStatus(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "ticketId"),
                                                        UuidArgument.getUuid(ctx, "statusId")))))))
                .then(Commands.literal("type")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("typeId", UuidArgument.uuid())
                                                .executes(ctx -> runTicketType(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        UuidArgument.getUuid(ctx, "ticketId"),
                                                        UuidArgument.getUuid(ctx, "typeId")))))));
    }

    // ===== project handlers =============================================

    private static int runCreate(CommandSourceStack source, String id, String name) {
        return mutation(source, ctx -> {
            Project created = new CreateProjectUseCase(ctx.repo).execute(ctx.actor.getUUID(), id, name);
            return Result.broadcast(created, "Created project " + created.id());
        });
    }

    private static int runList(CommandSourceStack source) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;

        List<Project> visible = new ProjectQueries(repo).listProjects(actor.getUUID());
        if (visible.isEmpty()) {
            report(source, "No projects you can view.");
            return 0;
        }
        visible.stream()
                .sorted(Comparator.comparing(Project::id))
                .forEach(p -> report(source, p.id() + " — " + p.name()));
        return visible.size();
    }

    private static int runDelete(CommandSourceStack source, String id) {
        return mutation(source, ctx -> {
            new DeleteProjectUseCase(ctx.repo).execute(ctx.actor.getUUID(), id);
            return Result.removal(id, "Deleted project " + id);
        });
    }

    private static int runRename(CommandSourceStack source, String id, String newName) {
        return mutation(source, ctx -> {
            Project updated = new RenameProjectUseCase(ctx.repo).execute(ctx.actor.getUUID(), id, newName);
            return Result.broadcast(updated, "Renamed " + id + " to " + newName);
        });
    }

    private static int runOwner(CommandSourceStack source, String id, ServerPlayer newOwner) {
        return mutation(source, ctx -> {
            Project updated = new TransferOwnershipUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), id, newOwner.getUUID());
            return Result.broadcast(updated, "Transferred " + id + " to " + newOwner.getName().getString());
        });
    }

    private static int runNonmembers(CommandSourceStack source, String id, boolean allow) {
        return mutation(source, ctx -> {
            Project updated = new SetAllowNonMembersUseCase(ctx.repo).execute(ctx.actor.getUUID(), id, allow);
            return Result.broadcast(updated, "Non-member access on " + id + ": " + allow);
        });
    }

    private static int runShow(CommandSourceStack source, String id) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;

        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), id).orElse(null);
            if (p == null) {
                source.sendFailure(Component.literal("No project '" + id + "'."));
                return 0;
            }
            report(source, "== " + p.id() + " — " + p.name() + " ==");
            source.sendSuccess(() -> Component.literal("Owner: ")
                    .append(ChatHelpers.playerName(source.getServer(), p.ownerUuid())), false);
            report(source, "Allow non-members: " + p.allowNonMembers());
            report(source, "Roles: " + p.roles().size());
            report(source, "Statuses: " + p.statuses().size());
            report(source, "Types: " + p.types().size());
            report(source, "Tickets: " + p.tickets().size());
            return 1;
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    // ===== role handlers ================================================

    private static int runRoleCreate(CommandSourceStack source, String projectId, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name")
                    .orElseThrow(() -> new SnbtPatch.Format("role create requires 'name'"));
            int color = p.optionalInt("color").orElse(0xFFFFFF);
            Set<Permission> perms = p.optionalPermissions("permissions").orElse(Set.of());
            new CreateRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, name, perms, color);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created role " + name);
        });
    }

    private static int runRoleUpdate(CommandSourceStack source, String projectId, UUID roleId, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            p.optionalString("name").ifPresent(name ->
                    new RenameRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, name));
            p.optionalInt("color").ifPresent(color ->
                    new UpdateRoleColorUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, color));
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated role " + roleId);
        });
    }

    private static int runRoleDelete(CommandSourceStack source, String projectId, UUID roleId) {
        return mutation(source, ctx -> {
            new DeleteRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted role " + roleId);
        });
    }

    private static int runRolePermissions(CommandSourceStack source, String projectId, UUID roleId, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            Set<Permission> perms = p.optionalPermissions("permissions")
                    .orElseThrow(() -> new SnbtPatch.Format("role permissions requires 'permissions' list"));
            new UpdateRolePermissionsUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, perms);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Set " + perms.size() + " permission(s) on role " + roleId);
        });
    }

    private static int runRoleAssign(CommandSourceStack source, String projectId, UUID roleId, ServerPlayer target) {
        return mutation(source, ctx -> {
            new AssignRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, target.getUUID(), roleId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Assigned role to " + target.getName().getString());
        });
    }

    private static int runRoleUnassign(CommandSourceStack source, String projectId, UUID roleId, ServerPlayer target) {
        return mutation(source, ctx -> {
            new UnassignRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, target.getUUID(), roleId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Unassigned role from " + target.getName().getString());
        });
    }

    private static int runRoleReorder(CommandSourceStack source, String projectId, String orderedIds) {
        return mutation(source, ctx -> {
            List<UUID> order = parseUuids(orderedIds);
            new ReorderRolesUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, order);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Reordered " + order.size() + " role(s)");
        });
    }

    // ===== status handlers ==============================================

    private static int runStatusCreate(CommandSourceStack source, String projectId, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name")
                    .orElseThrow(() -> new SnbtPatch.Format("status create requires 'name'"));
            int textColor = p.optionalInt("textColor").orElse(0xFFFFFF);
            int bgColor = p.optionalInt("backgroundColor").orElse(0x000000);
            new CreateStatusUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, name, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created status " + name);
        });
    }

    private static int runStatusUpdate(CommandSourceStack source, String projectId, UUID statusId, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = ctx.repo.find(projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            TicketStatus existing = current.findStatus(statusId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.STATUS, statusId.toString()));
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name").orElse(existing.name());
            int textColor = p.optionalInt("textColor").orElse(existing.textColor());
            int bgColor = p.optionalInt("backgroundColor").orElse(existing.backgroundColor());
            new UpdateStatusUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, statusId, name, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated status " + statusId);
        });
    }

    private static int runStatusDelete(CommandSourceStack source, String projectId, UUID statusId, UUID replacementId) {
        return mutation(source, ctx -> {
            new DeleteStatusUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, statusId, replacementId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted status " + statusId);
        });
    }

    private static int runStatusReorder(CommandSourceStack source, String projectId, String orderedIds) {
        return mutation(source, ctx -> {
            List<UUID> order = parseUuids(orderedIds);
            new ReorderStatusesUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, order);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Reordered " + order.size() + " status(es)");
        });
    }

    // ===== type handlers ================================================

    private static int runTypeCreate(CommandSourceStack source, String projectId, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name")
                    .orElseThrow(() -> new SnbtPatch.Format("type create requires 'name'"));
            int textColor = p.optionalInt("textColor").orElse(0xFFFFFF);
            int bgColor = p.optionalInt("backgroundColor").orElse(0x000000);
            new CreateTypeUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, name, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created type " + name);
        });
    }

    private static int runTypeUpdate(CommandSourceStack source, String projectId, UUID typeId, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = ctx.repo.find(projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            TicketType existing = current.findType(typeId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.TYPE, typeId.toString()));
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name").orElse(existing.name());
            int textColor = p.optionalInt("textColor").orElse(existing.textColor());
            int bgColor = p.optionalInt("backgroundColor").orElse(existing.backgroundColor());
            new UpdateTypeUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, typeId, name, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated type " + typeId);
        });
    }

    private static int runTypeDelete(CommandSourceStack source, String projectId, UUID typeId, UUID replacementId) {
        return mutation(source, ctx -> {
            new DeleteTypeUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, typeId, replacementId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted type " + typeId);
        });
    }

    private static int runTypeReorder(CommandSourceStack source, String projectId, String orderedIds) {
        return mutation(source, ctx -> {
            List<UUID> order = parseUuids(orderedIds);
            new ReorderTypesUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, order);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Reordered " + order.size() + " type(s)");
        });
    }

    // ===== ticket handlers ==============================================

    private static int runTicketCreate(CommandSourceStack source, String projectId, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = ctx.repo.find(projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            SnbtPatch p = new SnbtPatch(patch);
            String title = p.optionalString("title")
                    .orElseThrow(() -> new SnbtPatch.Format("ticket create requires 'title'"));
            String description = p.optionalString("description").orElse("");
            UUID assignee = p.optionalUuid("assignee").orElse(null);
            UUID statusId = p.optionalUuid("statusId").orElseGet(() -> firstStatusId(current));
            UUID typeId = p.optionalUuid("typeId").orElseGet(() -> firstTypeId(current));
            Ticket created = new CreateTicketUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, title, description, assignee, statusId, typeId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created ticket " + created.id());
        });
    }

    private static int runTicketUpdate(CommandSourceStack source, String projectId, UUID ticketId, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            // UpdateTicketUseCase already accepts nullable for patch semantics.
            String newTitle = p.optionalString("title").orElse(null);
            String newDescription = p.optionalString("description").orElse(null);
            new UpdateTicketUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, newTitle, newDescription);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated ticket " + ticketId);
        });
    }

    private static int runTicketDelete(CommandSourceStack source, String projectId, UUID ticketId) {
        return mutation(source, ctx -> {
            new DeleteTicketUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, ticketId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted ticket " + ticketId);
        });
    }

    private static int runTicketAssign(CommandSourceStack source, String projectId, UUID ticketId, ServerPlayer target) {
        return mutation(source, ctx -> {
            new AssignTicketUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, target.getUUID());
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Assigned ticket to " + target.getName().getString());
        });
    }

    private static int runTicketStatus(CommandSourceStack source, String projectId, UUID ticketId, UUID statusId) {
        return mutation(source, ctx -> {
            new ChangeTicketStatusUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, statusId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved ticket to status " + statusId);
        });
    }

    private static int runTicketType(CommandSourceStack source, String projectId, UUID ticketId, UUID typeId) {
        return mutation(source, ctx -> {
            new ChangeTicketTypeUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, typeId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Set ticket type to " + typeId);
        });
    }

    // ===== mutation scaffolding =========================================

    private record MutationCtx(ServerPlayer actor, JsonProjectRepository repo) {}

    private sealed interface Result permits Result.Broadcast, Result.Removal {
        static Result broadcast(Project project, String message) { return new Broadcast(project, message); }
        static Result removal(String projectId, String message) { return new Removal(projectId, message); }
        record Broadcast(Project project, String message) implements Result {}
        record Removal(String projectId, String message) implements Result {}
    }

    @FunctionalInterface
    private interface MutationBody {
        Result run(MutationCtx ctx) throws CommandSyntaxException;
    }

    private static int mutation(CommandSourceStack source, MutationBody body) {
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
        } catch (UseCaseException | SnbtPatch.Format e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    // ===== shared helpers ===============================================

    private static UUID firstStatusId(Project project) {
        List<TicketStatus> statuses = project.statuses();
        if (statuses.isEmpty()) throw new SnbtPatch.Format("project has no statuses");
        return statuses.get(0).id();
    }

    private static UUID firstTypeId(Project project) {
        List<TicketType> types = project.types();
        if (types.isEmpty()) throw new SnbtPatch.Format("project has no types");
        return types.get(0).id();
    }

    private static List<UUID> parseUuids(String input) {
        List<UUID> out = new ArrayList<>();
        for (String tok : input.trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            try {
                out.add(UUID.fromString(tok));
            } catch (IllegalArgumentException e) {
                throw new SnbtPatch.Format("invalid UUID: " + tok);
            }
        }
        return out;
    }

    private static void report(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
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
