package com.mossman.core.usecase.status;

import com.mossman.core.model.Project;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.ValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ReorderStatusesUseCase {

    private final ProjectRepository repository;

    public ReorderStatusesUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Project execute(UUID actor, String projectId, List<UUID> newOrder) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(newOrder, "newOrder");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);

        Set<UUID> existing = new HashSet<>();
        for (TicketStatus s : project.statuses()) existing.add(s.id());
        Set<UUID> orderSet = new HashSet<>(newOrder);
        if (orderSet.size() != newOrder.size()) {
            throw new ValidationException("reorder list contains duplicate ids");
        }
        if (!orderSet.equals(existing)) {
            throw new ValidationException("reorder list does not match existing statuses exactly");
        }

        List<TicketStatus> reordered = new ArrayList<>(newOrder.size());
        for (UUID id : newOrder) reordered.add(project.findStatus(id).orElseThrow());
        Project updated = project.withStatuses(reordered);
        repository.save(updated);
        return updated;
    }
}
