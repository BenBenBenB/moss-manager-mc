package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.permission.Permission;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateRoleUseCase {

    private final ProjectRepository repository;
    private final Supplier<UUID> idSupplier;

    public CreateRoleUseCase(ProjectRepository repository, Supplier<UUID> idSupplier) {
        this.repository = Objects.requireNonNull(repository);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public CreateRoleUseCase(ProjectRepository repository) {
        this(repository, UUID::randomUUID);
    }

    public Role execute(UUID actor, String projectId, String name, Set<Permission> grants, int color) {
        Objects.requireNonNull(actor, "actor");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        RolePermissions.requireManageRoles(project, actor);

        Role role = new Role(idSupplier.get(), name, grants, Set.of(), color);
        List<Role> next = new ArrayList<>(project.roles());
        // append at the tail; default sits at index 0 and stays at index 0.
        next.add(role);
        Project updated = project.withRoles(next);
        repository.save(updated);
        return role;
    }
}
