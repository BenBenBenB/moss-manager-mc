package com.mossman.command;

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
import com.mossman.core.usecase.role.UpdateRoleDenialsUseCase;
import com.mossman.core.usecase.role.UpdateRoleGrantsUseCase;
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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chat surface for every core use case, shaped per {@code docs/commands.md}.
 *
 * <p>Top-level layout:
 * <pre>
 *   /mossman project ... (list, view, create, delete, update, config{role,status,type})
 *   /mossman ticket ...  (list, view, create, delete, update)
 * </pre>
 *
 * <p>Every command targets a record by its human name (role, status, type)
 * or its per-project integer (ticket). {@link Suggestions} provides tab
 * completion for project IDs, names, ticket numbers, and Permission enum
 * values. SNBT patches drive update commands: only fields present in the
 * patch change; missing keys keep the current value.
 *
 * <p>Folded commands per the spec: {@code project update} carries
 * {@code name}/{@code allowNonMembers}/{@code owner} (the old rename,
 * nonmembers, owner verbs are gone). {@code ticket update} carries
 * {@code title}/{@code description}/{@code status}/{@code type}/{@code assignee}
 * (the old per-field ticket verbs are gone). {@code role/status/type order}
 * moves one record to a position; the old bulk reorder verb is gone.
 */
public final class MossmanCommand {

    private MossmanCommand() {}

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) ->
                dispatcher.register(Commands.literal("mossman")
                        .then(projectSubtree())
                        .then(ticketSubtree())));
    }

    // ===== /mossman project ============================================

    private static LiteralArgumentBuilder<CommandSourceStack> projectSubtree() {
        return Commands.literal("project")
                .then(Commands.literal("list")
                        .executes(ctx -> runProjectList(ctx.getSource())))
                .then(Commands.literal("view")
                        .then(projectIdArg()
                                .executes(ctx -> runProjectView(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> runProjectCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .executes(ctx -> runProjectDelete(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runProjectUpdate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("config")
                        .then(roleSubtree())
                        .then(statusSubtree())
                        .then(typeSubtree()));
    }

    // ===== /mossman project config role ================================

    private static LiteralArgumentBuilder<CommandSourceStack> roleSubtree() {
        return Commands.literal("role")
                .then(Commands.literal("list")
                        .then(projectIdArg()
                                .executes(ctx -> runRoleList(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(projectIdArg()
                                .then(roleIdArg()
                                        .executes(ctx -> runRoleView(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "roleId"))))))
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .then(Commands.argument("roleId", StringArgumentType.string())
                                        .executes(ctx -> runRoleCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "roleId"),
                                                null))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runRoleCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(roleIdArg()
                                        .executes(ctx -> runRoleDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "roleId"))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(roleIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runRoleUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("order")
                        .then(projectIdArg()
                                .then(roleIdArg()
                                        .then(Commands.argument("position", IntegerArgumentType.integer(1))
                                                .executes(ctx -> runRoleOrder(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        IntegerArgumentType.getInteger(ctx, "position")))))))
                .then(Commands.literal("setpermission")
                        .then(projectIdArg()
                                .then(roleIdArg()
                                        .then(Commands.argument("permissionId", StringArgumentType.word())
                                                .suggests(Suggestions.PERMISSIONS)
                                                .then(Commands.argument("level", StringArgumentType.word())
                                                        .suggests(Suggestions.PERMISSION_LEVELS)
                                                        .executes(ctx -> runRoleSetPermission(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "projectId"),
                                                                StringArgumentType.getString(ctx, "roleId"),
                                                                StringArgumentType.getString(ctx, "permissionId"),
                                                                StringArgumentType.getString(ctx, "level"))))))))
                .then(Commands.literal("assign")
                        .then(projectIdArg()
                                .then(roleIdArg()
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runRoleAssign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("unassign")
                        .then(projectIdArg()
                                .then(roleIdArg()
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> runRoleUnassign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        EntityArgument.getPlayer(ctx, "player")))))));
    }

    // ===== /mossman project config status ==============================

    private static LiteralArgumentBuilder<CommandSourceStack> statusSubtree() {
        return Commands.literal("status")
                .then(Commands.literal("list")
                        .then(projectIdArg()
                                .executes(ctx -> runStatusList(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(projectIdArg()
                                .then(statusIdArg()
                                        .executes(ctx -> runStatusView(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "statusId"))))))
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .then(Commands.argument("statusId", StringArgumentType.string())
                                        .executes(ctx -> runStatusCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "statusId"),
                                                null))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runStatusCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(statusIdArg()
                                        .executes(ctx -> runStatusDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "statusId"),
                                                null))
                                        .then(Commands.argument("replacementStatusId", StringArgumentType.string())
                                                .suggests(Suggestions.statusNames("projectId"))
                                                .executes(ctx -> runStatusDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        StringArgumentType.getString(ctx, "replacementStatusId")))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(statusIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runStatusUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("order")
                        .then(projectIdArg()
                                .then(statusIdArg()
                                        .then(Commands.argument("position", IntegerArgumentType.integer(1))
                                                .executes(ctx -> runStatusOrder(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "statusId"),
                                                        IntegerArgumentType.getInteger(ctx, "position")))))));
    }

    // ===== /mossman project config type (parallel to status) ===========

    private static LiteralArgumentBuilder<CommandSourceStack> typeSubtree() {
        return Commands.literal("type")
                .then(Commands.literal("list")
                        .then(projectIdArg()
                                .executes(ctx -> runTypeList(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(projectIdArg()
                                .then(typeIdArg()
                                        .executes(ctx -> runTypeView(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "typeId"))))))
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .then(Commands.argument("typeId", StringArgumentType.string())
                                        .executes(ctx -> runTypeCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "typeId"),
                                                null))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runTypeCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(typeIdArg()
                                        .executes(ctx -> runTypeDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "typeId"),
                                                null))
                                        .then(Commands.argument("replacementTypeId", StringArgumentType.string())
                                                .suggests(Suggestions.typeNames("projectId"))
                                                .executes(ctx -> runTypeDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        StringArgumentType.getString(ctx, "replacementTypeId")))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(typeIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runTypeUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("order")
                        .then(projectIdArg()
                                .then(typeIdArg()
                                        .then(Commands.argument("position", IntegerArgumentType.integer(1))
                                                .executes(ctx -> runTypeOrder(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        IntegerArgumentType.getInteger(ctx, "position")))))));
    }

    // ===== /mossman ticket =============================================
    // Ticket update patch keys: title, description, status (name), type
    // (name), assignee (UUID or online player name).
    private static LiteralArgumentBuilder<CommandSourceStack> ticketSubtree() {
        return Commands.literal("ticket")
                .then(Commands.literal("list")
                        .then(projectIdArg()
                                .executes(ctx -> runTicketList(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(projectIdArg()
                                .then(ticketIdArg()
                                        .executes(ctx -> runTicketView(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                IntegerArgumentType.getInteger(ctx, "ticketId"))))))
                .then(Commands.literal("create")
                        .then(projectIdArg()
                                .executes(ctx -> runTicketCreate(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId"),
                                        null))
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> runTicketCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                CompoundTagArgument.getCompoundTag(ctx, "patch"))))))
                .then(Commands.literal("delete")
                        .then(projectIdArg()
                                .then(ticketIdArg()
                                        .executes(ctx -> runTicketDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                IntegerArgumentType.getInteger(ctx, "ticketId"))))))
                .then(Commands.literal("update")
                        .then(projectIdArg()
                                .then(ticketIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> runTicketUpdate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        IntegerArgumentType.getInteger(ctx, "ticketId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))));
    }

    // ===== arg builders ================================================

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> projectIdArg() {
        return Commands.argument("projectId", StringArgumentType.word())
                .suggests(Suggestions.PROJECT_IDS);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> roleIdArg() {
        return Commands.argument("roleId", StringArgumentType.string())
                .suggests(Suggestions.roleNames("projectId"));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> statusIdArg() {
        return Commands.argument("statusId", StringArgumentType.string())
                .suggests(Suggestions.statusNames("projectId"));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> typeIdArg() {
        return Commands.argument("typeId", StringArgumentType.string())
                .suggests(Suggestions.typeNames("projectId"));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> ticketIdArg() {
        return Commands.argument("ticketId", IntegerArgumentType.integer(1))
                .suggests(Suggestions.ticketNumbers("projectId"));
    }

    // ===== project handlers ============================================

    private static int runProjectList(CommandSourceStack source) {
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
                .forEach(p -> reportComponent(source, Component.literal(p.id() + " — " + p.name() + " ")
                        .append(ChatHelpers.createRunLink("[view]",
                                "/mossman project view " + p.id(),
                                "View " + p.name(), ChatFormatting.GREEN))));
        return visible.size();
    }

    private static int runProjectView(CommandSourceStack source, String id) {
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
            reportComponent(source, Component.literal("Roles: " + p.roles().size() + " ")
                    .append(ChatHelpers.createRunLink("[list]",
                            "/mossman project config role list " + id, "List roles", ChatFormatting.GREEN)));
            reportComponent(source, Component.literal("Statuses: " + p.statuses().size() + " ")
                    .append(ChatHelpers.createRunLink("[list]",
                            "/mossman project config status list " + id, "List statuses", ChatFormatting.GREEN)));
            reportComponent(source, Component.literal("Types: " + p.types().size() + " ")
                    .append(ChatHelpers.createRunLink("[list]",
                            "/mossman project config type list " + id, "List types", ChatFormatting.GREEN)));
            if (p.tickets().isEmpty()) {
                report(source, "Tickets: 0");
            } else {
                reportComponent(source, Component.literal("Tickets: " + p.tickets().size() + " ")
                        .append(ChatHelpers.createRunLink("[list]",
                                "/mossman ticket list " + id, "List tickets", ChatFormatting.GREEN)));
                p.tickets().stream()
                        .sorted(Comparator.comparingInt(Ticket::number))
                        .forEach(t -> reportComponent(source,
                                Component.literal("  #" + t.number() + " — " + t.title() + " ")
                                        .append(ChatHelpers.createRunLink("[view]",
                                                "/mossman ticket view " + id + " " + t.number(),
                                                "View ticket #" + t.number(), ChatFormatting.GREEN))));
            }
            reportComponent(source, buttonRow(
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

    private static int runProjectCreate(CommandSourceStack source, String id, String name) {
        return mutation(source, ctx -> {
            Project template = ServerProjects.template().orElse(null);
            Project created = new CreateProjectUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), id, name, template);
            return Result.broadcast(created, "Created project " + created.id());
        });
    }

    private static int runProjectDelete(CommandSourceStack source, String id) {
        return mutation(source, ctx -> {
            new DeleteProjectUseCase(ctx.repo).execute(ctx.actor.getUUID(), id);
            return Result.removal(id, "Deleted project " + id);
        });
    }

    private static int runProjectUpdate(CommandSourceStack source, String id, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = new SnbtPatch(patch);
            p.optionalString("name").ifPresent(name ->
                    new RenameProjectUseCase(ctx.repo).execute(ctx.actor.getUUID(), id, name));
            p.optionalBool("allowNonMembers").ifPresent(allow ->
                    new SetAllowNonMembersUseCase(ctx.repo).execute(ctx.actor.getUUID(), id, allow));
            p.optionalString("owner").ifPresent(ref -> {
                UUID newOwner = resolvePlayerRef(source.getServer(), ref);
                new TransferOwnershipUseCase(ctx.repo).execute(ctx.actor.getUUID(), id, newOwner);
            });
            Project updated = ctx.repo.find(id).orElseThrow();
            return Result.broadcast(updated, "Updated project " + id);
        });
    }

    // ===== role handlers ===============================================

    private static int runRoleList(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            for (Role r : p.roles()) {
                reportComponent(source, Component.literal(r.name() + " ")
                        .append(ChatHelpers.createRunLink("[view]",
                                "/mossman project config role view " + projectId + " " + q(r.name()),
                                "View role " + r.name(), ChatFormatting.GREEN)));
            }
            reportComponent(source, buttonRow(
                    ChatHelpers.createSuggestLink("[create]",
                            "/mossman project config role create " + projectId + " ",
                            "Create a new role", ChatFormatting.YELLOW)));
            return p.roles().size();
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runRoleView(CommandSourceStack source, String projectId, String roleName) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            Role r = resolveRole(p, roleName);
            report(source, "== Role: " + r.name() + " ==");
            report(source, "Color: #" + String.format("%06X", r.color() & 0xFFFFFF));
            long members = p.memberRoles().values().stream()
                    .filter(set -> set.contains(r.id()))
                    .count();
            report(source, "Members: " + members);

            String qName = q(r.name());
            String cmdBase = "/mossman project config role";
            report(source, "Permissions:");
            for (Permission perm : Permission.values()) {
                PermissionLevel currentLevel = r.grants().contains(perm) ? PermissionLevel.GRANT
                        : r.denials().contains(perm) ? PermissionLevel.DENY
                        : PermissionLevel.INHERIT;
                String label = currentLevel == PermissionLevel.GRANT ? "GRANTED"
                        : currentLevel == PermissionLevel.DENY ? "DENIED"
                        : "inherit";
                MutableComponent line = Component.literal("  " + perm.name() + ": " + label + " ");
                for (PermissionLevel level : PermissionLevel.values()) {
                    if (level == currentLevel) continue;
                    line.append(ChatHelpers.createSuggestLink("[" + level + "]",
                            cmdBase + " setpermission " + projectId + " " + qName + " "
                                    + perm.name() + " " + level,
                            "Set " + perm.name() + " to " + level + " for " + r.name(),
                            ChatFormatting.YELLOW));
                    line.append(" ");
                }
                reportComponent(source, line);
            }
            reportComponent(source, buttonRow(
                    ChatHelpers.createSuggestLink("[update]",
                            cmdBase + " update " + projectId + " " + qName + " {}",
                            "Update role fields", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[order]",
                            cmdBase + " order " + projectId + " " + qName + " ",
                            "Reorder this role", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[assign]",
                            cmdBase + " assign " + projectId + " " + qName + " ",
                            "Assign this role to a player", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[unassign]",
                            cmdBase + " unassign " + projectId + " " + qName + " ",
                            "Unassign this role from a player", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[delete]",
                            cmdBase + " delete " + projectId + " " + qName,
                            "Delete this role", ChatFormatting.RED)));
            return 1;
        } catch (UseCaseException | IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runRoleCreate(CommandSourceStack source, String projectId, String roleName, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            int color = p.optionalInt("color").orElse(0xFFFFFF);
            Set<Permission> grants = p.optionalPermissions("grants").orElse(Set.of());
            Set<Permission> denials = p.optionalPermissions("denials").orElse(Set.of());
            Role created = new CreateRoleUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, roleName, grants, color);
            if (!denials.isEmpty()) {
                new UpdateRoleDenialsUseCase(ctx.repo)
                        .execute(ctx.actor.getUUID(), projectId, created.id(), denials);
            }
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created role " + roleName);
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

    private static int runRoleUpdate(CommandSourceStack source, String projectId, String roleName, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID roleId = resolveRole(current, roleName).id();
            SnbtPatch p = new SnbtPatch(patch);
            p.optionalString("name").ifPresent(name ->
                    new RenameRoleUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, name));
            p.optionalInt("color").ifPresent(color ->
                    new UpdateRoleColorUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, color));
            p.optionalPermissions("grants").ifPresent(grants ->
                    new UpdateRoleGrantsUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, grants));
            p.optionalPermissions("denials").ifPresent(denials ->
                    new UpdateRoleDenialsUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, roleId, denials));
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated role " + roleName);
        });
    }

    private static int runRoleOrder(CommandSourceStack source, String projectId, String roleName, int position) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            Role role = resolveRole(current, roleName);
            if (role.id().equals(current.defaultRoleId())) {
                throw new IllegalArgumentException("cannot reorder the default role; it is pinned at index 0");
            }
            int last = current.roles().size() - 1;
            if (position < 1 || position > last) {
                throw new IllegalArgumentException(
                        "position must be between 1 and " + last + " (inclusive)");
            }
            List<UUID> nonDefault = new ArrayList<>();
            for (int i = 1; i < current.roles().size(); i++) nonDefault.add(current.roles().get(i).id());
            nonDefault.remove(role.id());
            nonDefault.add(position - 1, role.id());
            new ReorderRolesUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, nonDefault);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved role " + roleName + " to position " + position);
        });
    }

    private enum PermissionLevel { GRANT, DENY, INHERIT }

    private static int runRoleSetPermission(CommandSourceStack source, String projectId, String roleName,
                                            String permName, String levelName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            Role role = resolveRole(current, roleName);
            Permission perm = parsePermission(permName);
            PermissionLevel target;
            try {
                target = PermissionLevel.valueOf(levelName.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "unknown level '" + levelName + "'; expected GRANT, DENY, or INHERIT");
            }

            PermissionLevel currentLevel = role.grants().contains(perm) ? PermissionLevel.GRANT
                    : role.denials().contains(perm) ? PermissionLevel.DENY
                    : PermissionLevel.INHERIT;
            if (currentLevel == target) {
                throw new IllegalArgumentException(
                        "role '" + role.name() + "' already has " + perm.name() + " at " + target);
            }

            Set<Permission> nextGrants = enumSetOf(role.grants());
            Set<Permission> nextDenials = enumSetOf(role.denials());
            nextGrants.remove(perm);
            nextDenials.remove(perm);
            if (target == PermissionLevel.GRANT) nextGrants.add(perm);
            else if (target == PermissionLevel.DENY) nextDenials.add(perm);

            if (!nextGrants.equals(role.grants())) {
                new UpdateRoleGrantsUseCase(ctx.repo)
                        .execute(ctx.actor.getUUID(), projectId, role.id(), nextGrants);
            }
            if (!nextDenials.equals(role.denials())) {
                new UpdateRoleDenialsUseCase(ctx.repo)
                        .execute(ctx.actor.getUUID(), projectId, role.id(), nextDenials);
            }
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated,
                    "Set " + perm.name() + " for " + roleName + " → " + target);
        });
    }

    private static Set<Permission> enumSetOf(Set<Permission> src) {
        return src.isEmpty()
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(src);
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

    // ===== status handlers =============================================

    private static int runStatusList(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            for (TicketStatus s : p.statuses()) {
                reportComponent(source, Component.literal(s.name() + " ")
                        .append(ChatHelpers.createRunLink("[view]",
                                "/mossman project config status view " + projectId + " " + q(s.name()),
                                "View status " + s.name(), ChatFormatting.GREEN)));
            }
            reportComponent(source, buttonRow(
                    ChatHelpers.createSuggestLink("[create]",
                            "/mossman project config status create " + projectId + " ",
                            "Create a new status", ChatFormatting.YELLOW)));
            return p.statuses().size();
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runStatusView(CommandSourceStack source, String projectId, String statusName) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            TicketStatus s = resolveStatus(p, statusName);
            report(source, "== Status: " + s.name() + " ==");
            report(source, "Text color: #" + String.format("%06X", s.textColor() & 0xFFFFFF));
            report(source, "Background:  #" + String.format("%06X", s.backgroundColor() & 0xFFFFFF));
            long tickets = p.tickets().stream().filter(t -> t.statusId().equals(s.id())).count();
            report(source, "Tickets in this status: " + tickets);
            String qName = q(s.name());
            reportComponent(source, buttonRow(
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

    private static int runStatusCreate(CommandSourceStack source, String projectId, String statusName, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            int textColor = p.optionalInt("textColor").orElse(0xFFFFFF);
            int bgColor = p.optionalInt("backgroundColor").orElse(0x000000);
            new CreateStatusUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, statusName, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created status " + statusName);
        });
    }

    private static int runStatusDelete(CommandSourceStack source, String projectId, String statusName,
                                       String replacementName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            TicketStatus toDelete = resolveStatus(current, statusName);
            TicketStatus replacement = replacementName == null
                    ? current.statuses().stream()
                            .filter(s -> !s.id().equals(toDelete.id()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "cannot delete the only status in '" + projectId + "'"))
                    : resolveStatus(current, replacementName);
            new DeleteStatusUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, toDelete.id(), replacement.id());
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated,
                    "Deleted status " + statusName + " (tickets moved to " + replacement.name() + ")");
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

    private static int runStatusOrder(CommandSourceStack source, String projectId, String statusName, int position) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID id = resolveStatus(current, statusName).id();
            List<UUID> order = moveToPosition(
                    current.statuses().stream().map(TicketStatus::id).collect(Collectors.toList()),
                    id, position);
            new ReorderStatusesUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, order);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved status " + statusName + " to position " + position);
        });
    }

    // ===== type handlers ===============================================

    private static int runTypeList(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            for (TicketType t : p.types()) {
                reportComponent(source, Component.literal(t.name() + " ")
                        .append(ChatHelpers.createRunLink("[view]",
                                "/mossman project config type view " + projectId + " " + q(t.name()),
                                "View type " + t.name(), ChatFormatting.GREEN)));
            }
            reportComponent(source, buttonRow(
                    ChatHelpers.createSuggestLink("[create]",
                            "/mossman project config type create " + projectId + " ",
                            "Create a new type", ChatFormatting.YELLOW)));
            return p.types().size();
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runTypeView(CommandSourceStack source, String projectId, String typeName) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            TicketType t = resolveType(p, typeName);
            report(source, "== Type: " + t.name() + " ==");
            report(source, "Text color: #" + String.format("%06X", t.textColor() & 0xFFFFFF));
            report(source, "Background:  #" + String.format("%06X", t.backgroundColor() & 0xFFFFFF));
            long tickets = p.tickets().stream().filter(tk -> tk.typeId().equals(t.id())).count();
            report(source, "Tickets of this type: " + tickets);
            String qName = q(t.name());
            reportComponent(source, buttonRow(
                    ChatHelpers.createSuggestLink("[update]",
                            "/mossman project config type update " + projectId + " " + qName + " {}",
                            "Update type fields", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[order]",
                            "/mossman project config type order " + projectId + " " + qName + " ",
                            "Reorder this type", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[delete]",
                            "/mossman project config type delete " + projectId + " " + qName,
                            "Delete this type", ChatFormatting.RED)));
            return 1;
        } catch (UseCaseException | IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runTypeCreate(CommandSourceStack source, String projectId, String typeName, CompoundTag patch) {
        return mutation(source, ctx -> {
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            int textColor = p.optionalInt("textColor").orElse(0xFFFFFF);
            int bgColor = p.optionalInt("backgroundColor").orElse(0x000000);
            new CreateTypeUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, typeName, textColor, bgColor);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created type " + typeName);
        });
    }

    private static int runTypeDelete(CommandSourceStack source, String projectId, String typeName,
                                     String replacementName) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            TicketType toDelete = resolveType(current, typeName);
            TicketType replacement = replacementName == null
                    ? current.types().stream()
                            .filter(t -> !t.id().equals(toDelete.id()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "cannot delete the only type in '" + projectId + "'"))
                    : resolveType(current, replacementName);
            new DeleteTypeUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, toDelete.id(), replacement.id());
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated,
                    "Deleted type " + typeName + " (tickets moved to " + replacement.name() + ")");
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

    private static int runTypeOrder(CommandSourceStack source, String projectId, String typeName, int position) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID id = resolveType(current, typeName).id();
            List<UUID> order = moveToPosition(
                    current.types().stream().map(TicketType::id).collect(Collectors.toList()),
                    id, position);
            new ReorderTypesUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, order);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved type " + typeName + " to position " + position);
        });
    }

    // ===== ticket handlers =============================================

    private static int runTicketList(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            if (p.tickets().isEmpty()) {
                report(source, "No tickets in " + projectId + ".");
                return 0;
            }
            p.tickets().stream()
                    .sorted(Comparator.comparingInt(Ticket::number))
                    .forEach(t -> reportComponent(source,
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

    private static int runTicketView(CommandSourceStack source, String projectId, int number) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            Ticket t = resolveTicket(p, number);
            TicketStatus s = p.findStatus(t.statusId()).orElse(null);
            TicketType ty = p.findType(t.typeId()).orElse(null);
            report(source, "== Ticket #" + t.number() + " — " + t.title() + " ==");
            report(source, "Status: " + (s == null ? "(missing)" : s.name()));
            report(source, "Type:   " + (ty == null ? "(missing)" : ty.name()));
            if (t.assigneeUuid() == null) {
                report(source, "Assignee: (unassigned)");
            } else {
                final UUID a = t.assigneeUuid();
                source.sendSuccess(() -> Component.literal("Assignee: ")
                        .append(ChatHelpers.playerName(source.getServer(), a)), false);
            }
            if (!t.description().isBlank()) {
                report(source, "Description: " + t.description());
            }
            reportComponent(source, buttonRow(
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

    private static int runTicketCreate(CommandSourceStack source, String projectId, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            String title = p.optionalString("title")
                    .orElseThrow(() -> new SnbtPatch.Format("ticket create requires 'title'"));
            String description = p.optionalString("description").orElse("");
            UUID assignee = p.optionalString("assignee")
                    .map(ref -> resolvePlayerRef(source.getServer(), ref))
                    .orElse(null);
            UUID statusId = p.optionalString("status")
                    .map(name -> resolveStatus(current, name).id())
                    .orElseGet(() -> firstStatusId(current));
            UUID typeId = p.optionalString("type")
                    .map(name -> resolveType(current, name).id())
                    .orElseGet(() -> firstTypeId(current));
            Ticket created = new CreateTicketUseCase(ctx.repo)
                    .execute(ctx.actor.getUUID(), projectId, title, description, assignee, statusId, typeId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created ticket #" + created.number());
        });
    }

    private static int runTicketDelete(CommandSourceStack source, String projectId, int number) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID ticketId = resolveTicket(current, number).id();
            new DeleteTicketUseCase(ctx.repo).execute(ctx.actor.getUUID(), projectId, ticketId);
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted ticket #" + number);
        });
    }

    private static int runTicketUpdate(CommandSourceStack source, String projectId, int number, CompoundTag patch) {
        return mutation(source, ctx -> {
            Project current = requireProject(ctx.repo, projectId);
            UUID ticketId = resolveTicket(current, number).id();
            SnbtPatch p = new SnbtPatch(patch);

            if (p.has("title") || p.has("description")) {
                String newTitle = p.optionalString("title").orElse(null);
                String newDescription = p.optionalString("description").orElse(null);
                new UpdateTicketUseCase(ctx.repo)
                        .execute(ctx.actor.getUUID(), projectId, ticketId, newTitle, newDescription);
            }
            p.optionalString("status").ifPresent(name -> {
                UUID statusId = resolveStatus(current, name).id();
                new ChangeTicketStatusUseCase(ctx.repo)
                        .execute(ctx.actor.getUUID(), projectId, ticketId, statusId);
            });
            p.optionalString("type").ifPresent(name -> {
                UUID typeId = resolveType(current, name).id();
                new ChangeTicketTypeUseCase(ctx.repo)
                        .execute(ctx.actor.getUUID(), projectId, ticketId, typeId);
            });
            if (p.has("assignee")) {
                UUID assignee = p.optionalString("assignee")
                        .map(ref -> ref.isEmpty() ? null : resolvePlayerRef(source.getServer(), ref))
                        .orElse(null);
                new AssignTicketUseCase(ctx.repo)
                        .execute(ctx.actor.getUUID(), projectId, ticketId, assignee);
            }
            Project updated = ctx.repo.find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated ticket #" + number);
        });
    }

    // ===== mutation scaffolding ========================================

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

    // ===== resolution helpers ==========================================

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

    private static Permission parsePermission(String name) {
        try {
            return Permission.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown permission '" + name + "'");
        }
    }

    /**
     * Accepts either a UUID string or the name of a currently-online player.
     * Online-only fallback for now; offline-by-name needs profile-cache work.
     */
    private static UUID resolvePlayerRef(MinecraftServer server, String ref) {
        try {
            return UUID.fromString(ref);
        } catch (IllegalArgumentException ignored) {
            // not a UUID — try name
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getGameProfile().name().equalsIgnoreCase(ref)) return p.getUUID();
        }
        throw new IllegalArgumentException("no online player named '" + ref
                + "' (use the player's UUID for offline targets)");
    }

    /**
     * Returns {@code currentOrder} with {@code targetId} moved to the 1-based
     * {@code position}. Position is clamped to {@code [1, size]}.
     */
    private static List<UUID> moveToPosition(List<UUID> currentOrder, UUID targetId, int position) {
        List<UUID> next = new ArrayList<>(currentOrder);
        if (!next.remove(targetId)) {
            throw new IllegalArgumentException("internal: target id missing from order list");
        }
        int clamped = Math.max(1, Math.min(position, next.size() + 1));
        next.add(clamped - 1, targetId);
        // Sanity check: no duplicates (use LinkedHashSet to preserve order).
        return new ArrayList<>(new LinkedHashSet<>(next));
    }

    // ===== shared helpers ==============================================

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

    private static void reportComponent(CommandSourceStack source, Component component) {
        source.sendSuccess(() -> component, false);
    }

    private static String q(String s) {
        if (s.indexOf(' ') < 0 && s.indexOf('"') < 0) return s;
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static MutableComponent buttonRow(Component... buttons) {
        MutableComponent row = Component.literal("  ");
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) row.append(" ");
            row.append(buttons[i]);
        }
        return row;
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
