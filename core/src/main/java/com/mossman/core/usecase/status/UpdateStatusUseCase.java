package com.mossman.core.usecase.status;

import com.mossman.core.model.Project;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class UpdateStatusUseCase {

    private final ProjectRepository repository;

    public UpdateStatusUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public TicketStatus execute(UUID actor, String projectId, UUID statusId,
                                String newName, int newTextColor, int newBackgroundColor) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(statusId, "statusId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_PROJECT);

        TicketStatus original = project.findStatus(statusId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.STATUS, statusId.toString()));
        TicketStatus updated = new TicketStatus(statusId, newName, newTextColor, newBackgroundColor);

        List<TicketStatus> next = new ArrayList<>(project.statuses().size());
        for (TicketStatus s : project.statuses()) next.add(s.id().equals(statusId) ? updated : s);
        repository.save(project.withStatuses(next));
        return updated;
    }
}
