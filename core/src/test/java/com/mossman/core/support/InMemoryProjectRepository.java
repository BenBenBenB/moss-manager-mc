package com.mossman.core.support;

import com.mossman.core.model.Project;
import com.mossman.core.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryProjectRepository implements ProjectRepository {

    private final Map<String, Project> byId = new LinkedHashMap<>();

    @Override
    public Optional<Project> find(String projectId) {
        return Optional.ofNullable(byId.get(projectId));
    }

    @Override
    public List<Project> list() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public void save(Project project) {
        Objects.requireNonNull(project, "project");
        byId.put(project.id(), project);
    }

    @Override
    public boolean delete(String projectId) {
        return byId.remove(projectId) != null;
    }

    public int size() {
        return byId.size();
    }
}
