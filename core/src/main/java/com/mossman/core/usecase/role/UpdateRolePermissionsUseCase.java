package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.permission.Permission;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class UpdateRolePermissionsUseCase {

    private final ProjectRepository repository;

    public UpdateRolePermissionsUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Role execute(UUID actor, String projectId, UUID roleId, Set<Permission> permissions) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(roleId, "roleId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        RolePermissions.requireCanEditRole(project, actor, roleId);

        Role original = project.findRole(roleId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.ROLE, roleId.toString()));
        Role updated = original.withPermissions(permissions);
        repository.save(project.withRoles(RenameRoleUseCase.replace(project.roles(), updated)));
        return updated;
    }
}
