package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;

import java.util.Objects;
import java.util.UUID;

public final class DeleteProjectUseCase {

    private final ProjectRepository repository;

    public DeleteProjectUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void execute(UUID actor, String projectId) {
        Objects.requireNonNull(actor, "actor");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        if (!project.ownerUuid().equals(actor)) {
            throw PermissionDeniedException.forOwnerOnly(projectId, actor);
        }
        repository.delete(projectId);
    }
}
