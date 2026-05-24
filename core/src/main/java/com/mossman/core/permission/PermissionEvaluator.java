package com.mossman.core.permission;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.usecase.PermissionDeniedException;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class PermissionEvaluator {

    private PermissionEvaluator() {}

    /**
     * Walks the project's roles list low→high. The default role (index 0) sets the
     * baseline for members and, when {@code allowNonMembers} is true, non-members.
     * Each subsequent role the actor is assigned to adds its grants and removes its
     * denials, so a higher-index role overrides lower-index grants on overlap.
     */
    public static Set<Permission> effective(Project project, UUID actor) {
        Set<UUID> assigned = project.memberRoles().getOrDefault(actor, Set.of());
        boolean isMember = !assigned.isEmpty();

        EnumSet<Permission> result = EnumSet.noneOf(Permission.class);

        for (int i = 0; i < project.roles().size(); i++) {
            Role role = project.roles().get(i);
            boolean isDefault = i == 0;
            boolean applies = isDefault
                    ? (isMember || project.allowNonMembers())
                    : assigned.contains(role.id());
            if (!applies) continue;
            result.addAll(role.grants());
            result.removeAll(role.denials());
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
