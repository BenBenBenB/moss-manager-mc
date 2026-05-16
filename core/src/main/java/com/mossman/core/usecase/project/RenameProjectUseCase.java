package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.ValidationException;

import java.util.Objects;
import java.util.UUID;

public final class RenameProjectUseCase {

    private final ProjectRepository repository;

    public RenameProjectUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Project execute(UUID actor, String projectId, String newName) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(newName, "newName");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);
        if (newName.isBlank()) {
            throw new ValidationException("name is blank");
        }
        Project updated = project.withName(newName);
        repository.save(updated);
        return updated;
    }
}
