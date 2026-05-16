package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class UnassignRoleUseCase {

    private final ProjectRepository repository;

    public UnassignRoleUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Project execute(UUID actor, String projectId, UUID target, UUID roleId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(roleId, "roleId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        RolePermissions.requireManageRoles(project, actor);

        Set<UUID> existing = project.memberRoles().getOrDefault(target, Set.of());
        if (!existing.contains(roleId)) {
            return project; // idempotent
        }
        Set<UUID> filtered = new HashSet<>(existing);
        filtered.remove(roleId);

        Map<UUID, Set<UUID>> next = new HashMap<>(project.memberRoles());
        if (filtered.isEmpty()) {
            next.remove(target);
        } else {
            next.put(target, Set.copyOf(filtered));
        }
        Project updated = project.withMemberRoles(next);
        repository.save(updated);
        return updated;
    }
}
