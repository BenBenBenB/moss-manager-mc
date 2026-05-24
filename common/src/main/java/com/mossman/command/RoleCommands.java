package com.mossman.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mossman.command.Mutations.Result;
import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.permission.Permission;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.core.usecase.role.AssignRoleUseCase;
import com.mossman.core.usecase.role.CreateRoleUseCase;
import com.mossman.core.usecase.role.DeleteRoleUseCase;
import com.mossman.core.usecase.role.RenameRoleUseCase;
import com.mossman.core.usecase.role.ReorderRolesUseCase;
import com.mossman.core.usecase.role.UnassignRoleUseCase;
import com.mossman.core.usecase.role.UpdateRoleColorUseCase;
import com.mossman.core.usecase.role.UpdateRoleDenialsUseCase;
import com.mossman.core.usecase.role.UpdateRoleGrantsUseCase;
import com.mossman.persistence.JsonProjectRepository;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /mossman project config role ...} — list / view / create / delete /
 * update / order / setpermission / assign / unassign.
 *
 * <p>{@code setpermission} carries the tri-state {@link PermissionLevel}
 * (GRANT / DENY / INHERIT) that lets one command flip the role's grant or
 * deny set for a single permission and clear the opposite side atomically.
 */
final class RoleCommands {

    private RoleCommands() {}

    enum PermissionLevel { GRANT, DENY, INHERIT }

    static LiteralArgumentBuilder<CommandSourceStack> subtree() {
        return Commands.literal("role")
                .then(Commands.literal("list")
                        .then(CommandArgs.projectIdArg()
                                .executes(ctx -> list(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.roleIdArg()
                                        .executes(ctx -> view(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "roleId"))))))
                .then(Commands.literal("create")
                        .then(CommandArgs.projectIdArg()
                                .then(Commands.argument("roleId", StringArgumentType.string())
                                        .executes(ctx -> create(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "roleId"),
                                                null))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> create(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.roleIdArg()
                                        .executes(ctx -> delete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "roleId"))))))
                .then(Commands.literal("update")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.roleIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> update(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("order")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.roleIdArg()
                                        .then(Commands.argument("position", IntegerArgumentType.integer(1))
                                                .executes(ctx -> order(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        IntegerArgumentType.getInteger(ctx, "position")))))))
                .then(Commands.literal("setpermission")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.roleIdArg()
                                        .then(Commands.argument("permissionId", StringArgumentType.word())
                                                .suggests(Suggestions.PERMISSIONS)
                                                .then(Commands.argument("level", StringArgumentType.word())
                                                        .suggests(Suggestions.PERMISSION_LEVELS)
                                                        .executes(ctx -> setPermission(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "projectId"),
                                                                StringArgumentType.getString(ctx, "roleId"),
                                                                StringArgumentType.getString(ctx, "permissionId"),
                                                                StringArgumentType.getString(ctx, "level"))))))))
                .then(Commands.literal("assign")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.roleIdArg()
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> assign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("unassign")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.roleIdArg()
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> unassign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "roleId"),
                                                        EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static int list(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            for (Role r : p.roles()) {
                Mutations.reportComponent(source, Component.empty()
                        .append(ChatHelpers.applyColor(Component.literal(r.name()), r.color()))
                        .append(" ")
                        .append(ChatHelpers.createRunLink("[view]",
                                "/mossman project config role view " + projectId + " " + Mutations.q(r.name()),
                                "View role " + r.name(), ChatFormatting.GREEN)));
            }
            Mutations.reportComponent(source, Mutations.buttonRow(
                    ChatHelpers.createSuggestLink("[create]",
                            "/mossman project config role create " + projectId + " ",
                            "Create a new role", ChatFormatting.YELLOW)));
            return p.roles().size();
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int view(CommandSourceStack source, String projectId, String roleName) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            Role r = Resolvers.resolveRole(p, roleName);
            Mutations.reportComponent(source, Component.literal("== Role: ")
                    .append(ChatHelpers.applyColor(Component.literal(r.name()), r.color()))
                    .append(" =="));
            Mutations.reportComponent(source, Component.literal("Color: ").append(ChatHelpers.colorSwatch(r.color())));
            long members = p.memberRoles().values().stream()
                    .filter(set -> set.contains(r.id()))
                    .count();
            Mutations.report(source, "Members: " + members);

            String qName = Mutations.q(r.name());
            String cmdBase = "/mossman project config role";
            Mutations.report(source, "Permissions:");
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
                Mutations.reportComponent(source, line);
            }
            Mutations.reportComponent(source, Mutations.buttonRow(
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

    private static int create(CommandSourceStack source, String projectId, String roleName, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            int color = p.optionalInt("color").orElse(0xFFFFFF);
            Set<Permission> grants = p.optionalPermissions("grants").orElse(Set.of());
            Set<Permission> denials = p.optionalPermissions("denials").orElse(Set.of());
            Role created = new CreateRoleUseCase(ctx.repo())
                    .execute(ctx.actor().getUUID(), projectId, roleName, grants, color);
            if (!denials.isEmpty()) {
                new UpdateRoleDenialsUseCase(ctx.repo())
                        .execute(ctx.actor().getUUID(), projectId, created.id(), denials);
            }
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created role " + roleName);
        });
    }

    private static int delete(CommandSourceStack source, String projectId, String roleName) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID roleId = Resolvers.resolveRole(current, roleName).id();
            new DeleteRoleUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, roleId);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Deleted role " + roleName);
        });
    }

    private static int update(CommandSourceStack source, String projectId, String roleName, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID roleId = Resolvers.resolveRole(current, roleName).id();
            SnbtPatch p = new SnbtPatch(patch);
            p.optionalString("name").ifPresent(name ->
                    new RenameRoleUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, roleId, name));
            p.optionalInt("color").ifPresent(color ->
                    new UpdateRoleColorUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, roleId, color));
            p.optionalPermissions("grants").ifPresent(grants ->
                    new UpdateRoleGrantsUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, roleId, grants));
            p.optionalPermissions("denials").ifPresent(denials ->
                    new UpdateRoleDenialsUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, roleId, denials));
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated role " + roleName);
        });
    }

    private static int order(CommandSourceStack source, String projectId, String roleName, int position) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            Role role = Resolvers.resolveRole(current, roleName);
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
            new ReorderRolesUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, nonDefault);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved role " + roleName + " to position " + position);
        });
    }

    private static int setPermission(CommandSourceStack source, String projectId, String roleName,
                                     String permName, String levelName) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            Role role = Resolvers.resolveRole(current, roleName);
            Permission perm = Resolvers.parsePermission(permName);
            PermissionLevel target;
            try {
                target = PermissionLevel.valueOf(levelName.toUpperCase(Locale.ROOT));
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
                new UpdateRoleGrantsUseCase(ctx.repo())
                        .execute(ctx.actor().getUUID(), projectId, role.id(), nextGrants);
            }
            if (!nextDenials.equals(role.denials())) {
                new UpdateRoleDenialsUseCase(ctx.repo())
                        .execute(ctx.actor().getUUID(), projectId, role.id(), nextDenials);
            }
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated,
                    "Set " + perm.name() + " for " + roleName + " → " + target);
        });
    }

    private static int assign(CommandSourceStack source, String projectId, String roleName, ServerPlayer target) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID roleId = Resolvers.resolveRole(current, roleName).id();
            new AssignRoleUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, target.getUUID(), roleId);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Assigned " + roleName + " to " + target.getName().getString());
        });
    }

    private static int unassign(CommandSourceStack source, String projectId, String roleName, ServerPlayer target) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID roleId = Resolvers.resolveRole(current, roleName).id();
            new UnassignRoleUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, target.getUUID(), roleId);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Unassigned " + roleName + " from " + target.getName().getString());
        });
    }

    private static Set<Permission> enumSetOf(Set<Permission> src) {
        return src.isEmpty()
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(src);
    }
}
