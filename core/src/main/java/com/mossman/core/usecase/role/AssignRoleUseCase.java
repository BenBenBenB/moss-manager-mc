package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.ValidationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class AssignRoleUseCase {

    private final ProjectRepository repository;

    public AssignRoleUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Project execute(UUID actor, String projectId, UUID target, UUID roleId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(roleId, "roleId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        RolePermissions.requireManageRoles(project, actor);

        if (roleId.equals(project.defaultRoleId())) {
            throw new ValidationException("cannot assign the default role; it applies implicitly");
        }
        if (project.findRole(roleId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.ROLE, roleId.toString());
        }

        Set<UUID> existing = project.memberRoles().getOrDefault(target, Set.of());
        Set<UUID> merged = new HashSet<>(existing);
        if (!merged.add(roleId)) {
            return project; // idempotent: no change
        }

        Map<UUID, Set<UUID>> next = new HashMap<>(project.memberRoles());
        next.put(target, Set.copyOf(merged));
        Project updated = project.withMemberRoles(next);
        repository.save(updated);
        return updated;
    }
}
