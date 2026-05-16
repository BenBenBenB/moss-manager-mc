package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.usecase.PermissionDeniedException;

import java.util.UUID;

/**
 * Shared permission-check helpers for role use cases. The default role is owner-only;
 * any other role requires MANAGE_ROLES.
 */
final class RolePermissions {

    private RolePermissions() {}

    static void requireCanEditRole(Project project, UUID actor, UUID roleId) {
        if (roleId.equals(project.defaultRoleId())) {
            if (!project.ownerUuid().equals(actor)) {
                throw PermissionDeniedException.forOwnerOnly(project.id(), actor);
            }
        } else {
            PermissionEvaluator.require(project, actor, Permission.MANAGE_ROLES);
        }
    }

    static void requireManageRoles(Project project, UUID actor) {
        PermissionEvaluator.require(project, actor, Permission.MANAGE_ROLES);
    }
}
