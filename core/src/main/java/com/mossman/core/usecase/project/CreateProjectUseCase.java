package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.ValidationException;

import java.util.Objects;
import java.util.UUID;

public final class CreateProjectUseCase {

    private final ProjectRepository repository;

    public CreateProjectUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Project execute(UUID actor, String id, String name) {
        return execute(actor, id, name, null);
    }

    public Project execute(UUID actor, String id, String name, Project template) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (repository.find(id).isPresent()) {
            throw new ValidationException("project already exists: " + id);
        }
        Project project = template == null
                ? Project.create(id, name, actor)
                : template.copyAsNew(id, name, actor);
        repository.save(project);
        return project;
    }
}
