package com.mossman.core.usecase.ticket;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.Objects;
import java.util.UUID;

public final class ChangeTicketStatusUseCase {

    private final ProjectRepository repository;

    public ChangeTicketStatusUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Ticket execute(UUID actor, String projectId, UUID ticketId, UUID newStatusId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(newStatusId, "newStatusId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.CHANGE_TICKET_STATUS);

        Ticket original = project.findTicket(ticketId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.TICKET, ticketId.toString()));
        if (project.findStatus(newStatusId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.STATUS, newStatusId.toString());
        }
        Ticket updated = original.withStatus(newStatusId);
        repository.save(project.replaceTicket(updated));
        return updated;
    }
}
