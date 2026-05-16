package com.mossman.core.permission;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.usecase.PermissionDeniedException;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class PermissionEvaluator {

    private PermissionEvaluator() {}

    public static Set<Permission> effective(Project project, UUID actor) {
        Set<UUID> assigned = project.memberRoles().getOrDefault(actor, Set.of());
        boolean isMember = !assigned.isEmpty();

        EnumSet<Permission> result = EnumSet.noneOf(Permission.class);

        if (isMember || project.allowNonMembers()) {
            result.addAll(project.defaultRole().permissions());
        }
        for (UUID roleId : assigned) {
            project.findRole(roleId).map(Role::permissions).ifPresent(result::addAll);
        }
        return result;
    }

    public static boolean has(Project project, UUID actor, Permission required) {
        return effective(project, actor).contains(required);
    }

    public static void require(Project project, UUID actor, Permission required) {
        if (!has(project, actor, required)) {
            throw PermissionDeniedException.forPermission(project.id(), actor, required);
        }
    }
}
