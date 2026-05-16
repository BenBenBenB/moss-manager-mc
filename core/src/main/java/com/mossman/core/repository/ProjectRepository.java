package com.mossman.core.repository;

import com.mossman.core.model.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Optional<Project> find(String projectId);

    List<Project> list();

    /** Upsert. */
    void save(Project project);

    /** @return true if a project with the given id was removed. */
    boolean delete(String projectId);
}
