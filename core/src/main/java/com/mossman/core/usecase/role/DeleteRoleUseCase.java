package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.ValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class DeleteRoleUseCase {

    private final ProjectRepository repository;

    public DeleteRoleUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void execute(UUID actor, String projectId, UUID roleId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(roleId, "roleId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        RolePermissions.requireManageRoles(project, actor);

        if (roleId.equals(project.defaultRoleId())) {
            throw new ValidationException("cannot delete the default role");
        }
        if (project.findRole(roleId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.ROLE, roleId.toString());
        }

        List<Role> nextRoles = new ArrayList<>(project.roles().size() - 1);
        for (Role r : project.roles()) if (!r.id().equals(roleId)) nextRoles.add(r);

        Map<UUID, Set<UUID>> nextMembers = new LinkedHashMap<>();
        for (Map.Entry<UUID, Set<UUID>> e : project.memberRoles().entrySet()) {
            Set<UUID> filtered = new HashSet<>(e.getValue());
            filtered.remove(roleId);
            if (!filtered.isEmpty()) {
                nextMembers.put(e.getKey(), Set.copyOf(filtered));
            }
        }

        repository.save(project.withRolesAndMembers(nextRoles, nextMembers));
    }
}
