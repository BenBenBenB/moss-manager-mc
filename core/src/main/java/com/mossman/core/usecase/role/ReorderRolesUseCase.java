package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.ValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ReorderRolesUseCase {

    private final ProjectRepository repository;

    public ReorderRolesUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /**
     * @param newOrder a permutation of the non-default role ids in the desired top-to-bottom order.
     *                 The default role stays pinned at index 0; higher indices have higher priority
     *                 in {@link com.mossman.core.permission.PermissionEvaluator}.
     */
    public Project execute(UUID actor, String projectId, List<UUID> newOrder) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(newOrder, "newOrder");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        RolePermissions.requireManageRoles(project, actor);

        Set<UUID> nonDefault = new HashSet<>();
        for (int i = 1; i < project.roles().size(); i++) nonDefault.add(project.roles().get(i).id());

        if (newOrder.contains(project.defaultRoleId())) {
            throw new ValidationException("default role must not appear in reorder list");
        }
        Set<UUID> orderSet = new HashSet<>(newOrder);
        if (orderSet.size() != newOrder.size()) {
            throw new ValidationException("reorder list contains duplicate ids");
        }
        if (!orderSet.equals(nonDefault)) {
            throw new ValidationException("reorder list does not match non-default roles exactly");
        }

        List<Role> reordered = new ArrayList<>(project.roles().size());
        reordered.add(project.defaultRole());
        for (UUID id : newOrder) reordered.add(project.findRole(id).orElseThrow());

        Project updated = project.withRoles(reordered);
        repository.save(updated);
        return updated;
    }
}
