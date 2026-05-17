package com.mossman.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Chat surface for every core use case. Same code path as the C2S handler:
 * both call the same use cases against {@link ServerProjects}'s repo and
 * broadcast through {@link ServerProjectSync}, so observable client state
 * is identical regardless of entry point.
 *
 * <p>Targeting uses human-readable identifiers: project IDs (already
 * word-shaped), case-insensitive role/status/type names (first match wins
 * if names collide), and per-project ticket numbers. Tab completion is
 * wired via {@link Suggestions} so users can discover valid values
 * inline instead of copying UUIDs out of JSON.
 *
 * <p>Update- and create-style leaves still take SNBT compound patches.
 * Missing keys mean "leave the field alone". Distinct semantic operations
 * (assign/unassign/permissions/reorder/delete) get their own leaves.
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
                .then(projectIdArg()
                        .executes(ctx -> runDelete(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "projectId"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> renameLeaf() {
        return Commands.literal("rename")
                .then(projectIdArg()
                        .then(Commands.argument("newName", StringArgumentType.greedyString())
                                .executes(ctx -> runRename(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId"),
                                        StringArgumentType.getString(ctx, "newName")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ownerLeaf() {
        return Commands.literal("owner")
                .then(projectIdArg()
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> runOwner(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId"),
                                        EntityArgument.getPlayer(ctx, "player")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nonmembersLeaf() {
        return Commands.literal("nonmembers")
                .then(projectIdArg()
                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                .executes(ctx -> runNonmembers(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId"),
                                        BoolArgumentType.getBool(ctx, "allow")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> showLeaf() {
        return Commands.literal("show")
                .then(projectIdArg()
                        .executes(ctx -> runShow(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "projectId"))));
    }

    // ===== role subtree =================================================
    // SNBT shapes:
    //   create: {name:"Reviewer", color:0xFFAA00, permissions:["VIEW_PROJECT", ...]}
    //   update: any subset of {name, color}  (patch)
    //   permissions: {permissions:[...]} — full Set replacement
    private static LiteralArgumentBuilder<CommandSourceStack> roleSubtree() {
        return Commands.literal("role")
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runRoleCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(Commands.argument("role", StringArgumentType.string())
                                        .suggests(Suggestions.roleNames("projectId"))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runRoleUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "role"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(Commands.argument("role", StringArgumentType.string())
                                        .suggests(Suggestions.roleNames("projectId"))
                                        .executes(ctx -> runRoleDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "role"))))))
                .then(Commands.literal("permissions")
                        .then(projectIdArg()
                                .then(Commands.argument("role", StringArgumentType.string())
                                        .suggests(Suggestions.roleNames("projectId"))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runRolePermissions(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "role"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("assign")
                        .then(projectIdArg()
                                .then(Commands.argument("role", StringArgumentType.string())
                                        .suggests(Suggestions.roleNames("projectId"))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runRoleAssign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "role"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("unassign")
                        .then(projectIdArg()
                                .then(Commands.argument("role", StringArgumentType.string())
                                        .suggests(Suggestions.roleNames("projectId"))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runRoleUnassign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "role"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("reorder")
                        // No tab completion on multi-token greedy strings; names
                        // with spaces aren't supported via this path — rename
                        // first or use a single-word alias.
                        .then(projectIdArg()
                                .then(Commands.argument("orderedNames", StringArgumentType.greedyString())
                                        .executes(ctx -> runRoleReorder(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "orderedNames"))))));
    }

    // ===== status subtree ===============================================
    // SNBT shapes:
    //   create: {name:"In Review", textColor:0xFFFFFF, backgroundColor:0x6B21A8}
    //   update: any subset of those keys (patch)
    private static LiteralArgumentBuilder<CommandSourceStack> statusSubtree() {
        return Commands.literal("status")
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runStatusCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(Commands.argument("status", StringArgumentType.string())
                                        .suggests(Suggestions.statusNames("projectId"))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runStatusUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "status"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(Commands.argument("status", StringArgumentType.string())
                                        .suggests(Suggestions.statusNames("projectId"))
                                        .then(Commands.argument("replacement", StringArgumentType.string())
                                                .suggests(Suggestions.statusNames("projectId"))
                                                .executes(ctx -> runStatusDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "status"),
                                                        StringArgumentType.getString(ctx, "replacement")))))))
                .then(Commands.literal("reorder")
                        .then(projectIdArg()
                                .then(Commands.argument("orderedNames", StringArgumentType.greedyString())
                                        .executes(ctx -> runStatusReorder(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "orderedNames"))))));
    }

    // ===== type subtree (parallel to status) ============================
    private static LiteralArgumentBuilder<CommandSourceStack> typeSubtree() {
        return Commands.literal("type")
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runTypeCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests(Suggestions.typeNames("projectId"))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runTypeUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests(Suggestions.typeNames("projectId"))
                                        .then(Commands.argument("replacement", StringArgumentType.string())
                                                .suggests(Suggestions.typeNames("projectId"))
                                                .executes(ctx -> runTypeDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        StringArgumentType.getString(ctx, "replacement")))))))
                .then(Commands.literal("reorder")
                        .then(projectIdArg()
                                .then(Commands.argument("orderedNames", StringArgumentType.greedyString())
                                        .executes(ctx -> runTypeReorder(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "orderedNames"))))));
    }

    // ===== ticket subtree ===============================================
    // SNBT shapes:
    //   create: {title, description?, assignee?, statusId?, typeId?}
    //   update: any subset of {title, description}  (already patch-style in the use case)
    // Note: statusId/typeId/assignee inside SNBT are still UUIDs because
    // SNBT compounds can't carry suggestions. Day-to-day flow uses /mossman
    // ticket {status,type,assign} commands which DO accept names.
    private static LiteralArgumentBuilder<CommandSourceStack> ticketSubtree() {
        return Commands.literal("ticket")
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runTicketCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(Commands.argument("ticket", IntegerArgumentType.integer(1))
                                        .suggests(Suggestions.ticketNumbers("projectId"))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runTicketUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        IntegerArgumentType.getInteger(ctx, "ticket"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(Commands.argument("ticket", IntegerArgumentType.integer(1))
                                        .suggests(Suggestions.ticketNumbers("projectId"))
                                        .executes(ctx -> runTicketDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                IntegerArgumentType.getInteger(ctx, "ticket"))))))
                .then(Commands.literal("assign")
                        .then(projectIdArg()
                                .then(Commands.argument("ticket", IntegerArgumentType.integer(1))
                                        .suggests(Suggestions.ticketNumbers("projectId"))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runTicketAssign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        IntegerArgumentType.getInteger(ctx, "ticket"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("status")
                        .then(projectIdArg()
                                .then(Commands.argument("ticket", IntegerArgumentType.integer(1))
                                        .suggests(Suggestions.ticketNumbers("projectId"))
                                        .then(Commands.argument("status", StringArgumentType.string())
                                                .suggests(Suggestions.statusNames("projectId"))
                                                .executes(ctx -> runTicketStatus(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        IntegerArgumentType.getInteger(ctx, "ticket"),
                                                        StringArgumentType.getString(ctx, "status")))))))
                .then(Commands.literal("type")
                        .then(projectIdArg()
                                .then(Commands.argument("ticket", IntegerArgumentType.integer(1))
                                        .suggests(Suggestions.ticketNumbers("projectId"))
                                        .then(Commands.argument("type", StringArgumentType.string())
                                                .suggests(Suggestions.typeNames("projectId"))
                                                .executes(ctx -> runTicketType(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        IntegerArgumentType.getInteger(ctx, "ticket"),
                                                        StringArgumentType.getString(ctx, "type")))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> projectIdArg() {
        return Commands.argument("projectId", StringArgumentType.word())
                .suggests(Suggestions.PROJECT_IDS);
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
            if (p.tickets().isEmpty()) {
                report(source, "Tickets: 0");
            } else {
                report(source, "Tickets: " + p.tickets().size());
                p.tickets().stream()
                        .sorted(Comparator.comparingInt(Ticket::number))
                        .forEach(t -> report(source, "  #" + t.number() + " — " + t.title()));
            }
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

    private static int runRoleUpdate(CommandSourceStack source, String projectId, String roleName, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID roleId = resolveRole(current, roleName).id();
            SnbtPatch p = new SnbtPatch(patch);
            p.optionalString("name").ifPresent(name ->
                    new RenameRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, name));
            p.optionalInt("color").ifPresent(color ->
                    new UpdateRoleColorUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, color));
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated role " + roleName);
        });
    }

    private static int runRoleDelete(CommandSourceStack source, String projectId, String roleName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID roleId = resolveRole(current, roleName).id();
            new DeleteRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted role " + roleName);
        });
    }

    private static int runRolePermissions(CommandSourceStack source, String projectId, String roleName, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID roleId = resolveRole(current, roleName).id();
            SnbtPatch p = new SnbtPatch(patch);
            Set<Permission> perms = p.optionalPermissions("permissions")
                    .orElseThrow(() -> new SnbtPatch.Format("role permissions requires 'permissions' list"));
            new UpdateRolePermissionsUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, perms);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Set " + perms.size() + " permission(s) on role " + roleName);
        });
    }

    private static int runRoleAssign(CommandSourceStack source, String projectId, String roleName, ServerPlayer target) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID roleId = resolveRole(current, roleName).id();
            new AssignRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, target.getUUID(), roleId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Assigned " + roleName + " to " + target.getName().getString());
        });
    }

    private static int runRoleUnassign(CommandSourceStack source, String projectId, String roleName, ServerPlayer target) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID roleId = resolveRole(current, roleName).id();
            new UnassignRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, target.getUUID(), roleId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Unassigned " + roleName + " from " + target.getName().getString());
        });
    }

    private static int runRoleReorder(CommandSourceStack source, String projectId, String orderedNames) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            List<UUID> order = parseRoleOrder(current, orderedNames);
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

    private static int runStatusUpdate(CommandSourceStack source, String projectId, String statusName, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            TicketStatus existing = resolveStatus(current, statusName);
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name").orElse(existing.name());
            int textColor = p.optionalInt("textColor").orElse(existing.textColor());
            int bgColor = p.optionalInt("backgroundColor").orElse(existing.backgroundColor());
            new UpdateStatusUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, existing.id(), name, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated status " + statusName);
        });
    }

    private static int runStatusDelete(CommandSourceStack source, String projectId,
                                       String statusName, String replacementName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID statusId = resolveStatus(current, statusName).id();
            UUID replacementId = resolveStatus(current, replacementName).id();
            new DeleteStatusUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, statusId, replacementId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted status " + statusName);
        });
    }

    private static int runStatusReorder(CommandSourceStack source, String projectId, String orderedNames) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            List<UUID> order = parseStatusOrder(current, orderedNames);
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

    private static int runTypeUpdate(CommandSourceStack source, String projectId, String typeName, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            TicketType existing = resolveType(current, typeName);
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name").orElse(existing.name());
            int textColor = p.optionalInt("textColor").orElse(existing.textColor());
            int bgColor = p.optionalInt("backgroundColor").orElse(existing.backgroundColor());
            new UpdateTypeUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, existing.id(), name, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated type " + typeName);
        });
    }

    private static int runTypeDelete(CommandSourceStack source, String projectId,
                                     String typeName, String replacementName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID typeId = resolveType(current, typeName).id();
            UUID replacementId = resolveType(current, replacementName).id();
            new DeleteTypeUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, typeId, replacementId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted type " + typeName);
        });
    }

    private static int runTypeReorder(CommandSourceStack source, String projectId, String orderedNames) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            List<UUID> order = parseTypeOrder(current, orderedNames);
            new ReorderTypesUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, order);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Reordered " + order.size() + " type(s)");
        });
    }

    // ===== ticket handlers ==============================================

    private static int runTicketCreate(CommandSourceStack source, String projectId, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
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
            return Result.broadcast(updated, "Created ticket #" + created.number());
        });
    }

    private static int runTicketUpdate(CommandSourceStack source, String projectId, int ticketNumber, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID ticketId = resolveTicket(current, ticketNumber).id();
            SnbtPatch p = new SnbtPatch(patch);
            String newTitle = p.optionalString("title").orElse(null);
            String newDescription = p.optionalString("description").orElse(null);
            new UpdateTicketUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, newTitle, newDescription);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated ticket #" + ticketNumber);
        });
    }

    private static int runTicketDelete(CommandSourceStack source, String projectId, int ticketNumber) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID ticketId = resolveTicket(current, ticketNumber).id();
            new DeleteTicketUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, ticketId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted ticket #" + ticketNumber);
        });
    }

    private static int runTicketAssign(CommandSourceStack source, String projectId, int ticketNumber, ServerPlayer target) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID ticketId = resolveTicket(current, ticketNumber).id();
            new AssignTicketUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, target.getUUID());
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Assigned ticket #" + ticketNumber + " to " + target.getName().getString());
        });
    }

    private static int runTicketStatus(CommandSourceStack source, String projectId, int ticketNumber, String statusName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID ticketId = resolveTicket(current, ticketNumber).id();
            UUID statusId = resolveStatus(current, statusName).id();
            new ChangeTicketStatusUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, statusId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved ticket #" + ticketNumber + " to " + statusName);
        });
    }

    private static int runTicketType(CommandSourceStack source, String projectId, int ticketNumber, String typeName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID ticketId = resolveTicket(current, ticketNumber).id();
            UUID typeId = resolveType(current, typeName).id();
            new ChangeTicketTypeUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, ticketId, typeId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Set ticket #" + ticketNumber + " type to " + typeName);
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
        } catch (UseCaseException | IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    // ===== resolution helpers ===========================================

    private static Project requireProject(JsonProjectRepository repo, String projectId) {
        return repo.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
    }

    private static Role resolveRole(Project project, String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        return project.roles().stream()
                .filter(r -> r.name().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no role named '" + name + "' in project '" + project.id() + "'"));
    }

    private static TicketStatus resolveStatus(Project project, String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        return project.statuses().stream()
                .filter(s -> s.name().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no status named '" + name + "' in project '" + project.id() + "'"));
    }

    private static TicketType resolveType(Project project, String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        return project.types().stream()
                .filter(t -> t.name().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no type named '" + name + "' in project '" + project.id() + "'"));
    }

    private static Ticket resolveTicket(Project project, int number) {
        return project.tickets().stream()
                .filter(t -> t.number() == number)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no ticket #" + number + " in project '" + project.id() + "'"));
    }

    private static List<UUID> parseRoleOrder(Project project, String input) {
        List<UUID> out = new ArrayList<>();
        for (String tok : input.trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            out.add(resolveRole(project, tok).id());
        }
        return out;
    }

    private static List<UUID> parseStatusOrder(Project project, String input) {
        List<UUID> out = new ArrayList<>();
        for (String tok : input.trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            out.add(resolveStatus(project, tok).id());
        }
        return out;
    }

    private static List<UUID> parseTypeOrder(Project project, String input) {
        List<UUID> out = new ArrayList<>();
        for (String tok : input.trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            out.add(resolveType(project, tok).id());
        }
        return out;
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
