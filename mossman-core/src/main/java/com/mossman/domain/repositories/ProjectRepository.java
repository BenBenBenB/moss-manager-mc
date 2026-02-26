package com.mossman.domain.repositories;

import com.mossman.domain.entities.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);
    Optional<Project> findById(long id);
    List<Project> findAll(int offset, int limit);
    List<Project> findAllForUser(java.util.UUID userId, int offset, int limit);
    long countAll();
    long countAllForUser(java.util.UUID userId);
    void delete(long id);
}
