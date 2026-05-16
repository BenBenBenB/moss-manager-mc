package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ProjectQueries {

    private final ProjectRepository repository;

    public ProjectQueries(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /**
     * Returns the project if it exists and the actor can view it.
     * - Project does not exist → empty Optional.
     * - Project exists, actor lacks VIEW_PROJECT → throws PermissionDeniedException.
     */
    public Optional<Project> getProject(UUID actor, String projectId) {
        Objects.requireNonNull(actor, "actor");
        Optional<Project> maybe = repository.find(projectId);
        if (maybe.isEmpty()) return Optional.empty();
        Project project = maybe.get();
        PermissionEvaluator.require(project, actor, Permission.VIEW_PROJECT);
        return Optional.of(project);
    }

    /** Returns all projects where the actor has at least VIEW_PROJECT. */
    public List<Project> listProjects(UUID actor) {
        Objects.requireNonNull(actor, "actor");
        List<Project> visible = new ArrayList<>();
        for (Project p : repository.list()) {
            if (PermissionEvaluator.has(p, actor, Permission.VIEW_PROJECT)) {
                visible.add(p);
            }
        }
        return visible;
    }
}
