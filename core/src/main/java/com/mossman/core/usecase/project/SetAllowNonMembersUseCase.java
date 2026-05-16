package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.Objects;
import java.util.UUID;

public final class SetAllowNonMembersUseCase {

    private final ProjectRepository repository;

    public SetAllowNonMembersUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Project execute(UUID actor, String projectId, boolean allow) {
        Objects.requireNonNull(actor, "actor");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.MANAGE_ROLES);
        Project updated = project.withAllowNonMembers(allow);
        repository.save(updated);
        return updated;
    }
}
