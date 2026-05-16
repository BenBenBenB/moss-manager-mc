package com.mossman.core.usecase.ticket;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.Objects;
import java.util.UUID;

public final class AssignTicketUseCase {

    private final ProjectRepository repository;

    public AssignTicketUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /** Pass null to unassign. */
    public Ticket execute(UUID actor, String projectId, UUID ticketId, UUID assignee) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ticketId, "ticketId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.ASSIGN_TICKETS);

        Ticket original = project.findTicket(ticketId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.TICKET, ticketId.toString()));
        Ticket updated = original.withAssignee(assignee);
        repository.save(project.replaceTicket(updated));
        return updated;
    }
}
