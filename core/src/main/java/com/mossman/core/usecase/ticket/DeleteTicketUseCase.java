package com.mossman.core.usecase.ticket;

import com.mossman.core.model.Project;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.Objects;
import java.util.UUID;

public final class DeleteTicketUseCase {

    private final ProjectRepository repository;

    public DeleteTicketUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void execute(UUID actor, String projectId, UUID ticketId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ticketId, "ticketId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.DELETE_TICKETS);
        if (project.findTicket(ticketId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.TICKET, ticketId.toString());
        }
        repository.save(project.removeTicket(ticketId));
    }
}
