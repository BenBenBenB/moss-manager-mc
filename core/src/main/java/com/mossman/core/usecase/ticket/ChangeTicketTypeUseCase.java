package com.mossman.core.usecase.ticket;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.Objects;
import java.util.UUID;

public final class ChangeTicketTypeUseCase {

    private final ProjectRepository repository;

    public ChangeTicketTypeUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Ticket execute(UUID actor, String projectId, UUID ticketId, UUID newTypeId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(newTypeId, "newTypeId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_TICKETS);

        Ticket original = project.findTicket(ticketId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.TICKET, ticketId.toString()));
        if (project.findType(newTypeId).isEmpty()) {
            throw new NotFoundException(NotFoundException.Kind.TYPE, newTypeId.toString());
        }
        Ticket updated = original.withType(newTypeId);
        repository.save(project.replaceTicket(updated));
        return updated;
    }
}
