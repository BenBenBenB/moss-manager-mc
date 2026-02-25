package com.mossman.domain.auth;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;

import java.util.Optional;
import java.util.UUID;

public class PermissionChecker {

    /**
     * Returns the effective permission for a user on a project.
     * If the user is a member of the project, their member permission is used.
     * Otherwise, the project's externalUserPermission is used.
     */
    public static Permission getEffectivePermission(Project project, UUID userId) {
        // Special case: console or OP might be handled via higher level check
        Optional<Member> memberOpt = project.getMembers().stream()
                .filter(m -> m.uuid().equals(userId))
                .findFirst();
        return memberOpt.map(Member::permission).orElse(project.getExternalUserPermission());
    }

    /**
     * Higher-level check that considers console/OP status.
     */
    public static boolean hasPermission(Project project, net.minecraft.server.command.ServerCommandSource source, Permission required) {
        if (source.getPlayer() == null) return true; // Console always has access
        Permission effective = getEffectivePermission(project, source.getPlayer().getUuid());
        return isAtLeast(effective, required);
    }

    public static void require(Project project, net.minecraft.server.command.ServerCommandSource source, Permission required) {
        if (!hasPermission(project, source, required)) {
            throw new SecurityException("Insufficient permission: " + required + " required");
        }
    }

    private static boolean isAtLeast(Permission effective, Permission required) {
        if (effective == Permission.OWNER) return true;
        if (effective == Permission.ADMIN) return required != Permission.OWNER;
        if (effective == Permission.EDITOR) return required != Permission.OWNER && required != Permission.ADMIN;
        if (effective == Permission.CREATOR) return required == Permission.CREATOR || required == Permission.VIEWER;
        if (effective == Permission.VIEWER) return required == Permission.VIEWER;
        return false;
    }

    public static boolean canView(Project project, net.minecraft.server.command.ServerCommandSource source) {
        if (source.getPlayer() == null) return true; // Console always has access
        return getEffectivePermission(project, source.getPlayer().getUuid()) != Permission.FORBID;
    }

    public static boolean isOwner(Project project, net.minecraft.server.command.ServerCommandSource source) {
        if (source.getPlayer() == null) return true; // Console always has access
        return getEffectivePermission(project, source.getPlayer().getUuid()) == Permission.OWNER;
    }

    /**
     * Domain-level helpers for Use Cases (using UUID)
     */
    public static void requireProjectAdmin(Project project, UUID requesterId) {
        Permission effective = getEffectivePermission(project, requesterId);
        if (effective != Permission.ADMIN && effective != Permission.OWNER) {
            throw new SecurityException("Insufficient permission: ADMIN or OWNER required");
        }
    }

    public static void requireProjectEditor(Project project, UUID requesterId) {
        Permission effective = getEffectivePermission(project, requesterId);
        if (!isAtLeast(effective, Permission.EDITOR)) {
            throw new SecurityException("Insufficient permission: EDITOR required");
        }
    }
}
