package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import com.mossman.core.usecase.ValidationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class TransferOwnershipUseCase {

    private final ProjectRepository repository;

    public TransferOwnershipUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Project execute(UUID actor, String projectId, UUID newOwner) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(newOwner, "newOwner");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        if (!project.ownerUuid().equals(actor)) {
            throw PermissionDeniedException.forOwnerOnly(projectId, actor);
        }
        if (newOwner.equals(actor)) {
            throw new ValidationException("new owner is already the owner");
        }

        Set<UUID> oldOwnerRoles = project.memberRoles().getOrDefault(actor, Set.of());
        Set<UUID> existingNewOwnerRoles = project.memberRoles().getOrDefault(newOwner, Set.of());

        Map<UUID, Set<UUID>> nextRoles = new HashMap<>(project.memberRoles());
        if (oldOwnerRoles.isEmpty() && existingNewOwnerRoles.isEmpty()) {
            nextRoles.remove(newOwner);
        } else {
            Set<UUID> union = new HashSet<>(existingNewOwnerRoles);
            union.addAll(oldOwnerRoles);
            nextRoles.put(newOwner, Set.copyOf(union));
        }

        Project updated = project.withMemberRoles(nextRoles).withOwner(newOwner);
        repository.save(updated);
        return updated;
    }
}
