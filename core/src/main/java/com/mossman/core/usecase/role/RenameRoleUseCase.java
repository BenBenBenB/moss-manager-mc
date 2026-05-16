package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RenameRoleUseCase {

    private final ProjectRepository repository;

    public RenameRoleUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Role execute(UUID actor, String projectId, UUID roleId, String newName) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(roleId, "roleId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        RolePermissions.requireCanEditRole(project, actor, roleId);

        Role original = project.findRole(roleId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.ROLE, roleId.toString()));
        Role updated = original.withName(newName);
        repository.save(project.withRoles(replace(project.roles(), updated)));
        return updated;
    }

    static List<Role> replace(List<Role> roles, Role updated) {
        List<Role> next = new ArrayList<>(roles.size());
        for (Role r : roles) {
            next.add(r.id().equals(updated.id()) ? updated : r);
        }
        return next;
    }
}
