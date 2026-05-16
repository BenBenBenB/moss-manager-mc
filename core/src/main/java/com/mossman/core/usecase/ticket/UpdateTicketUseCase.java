package com.mossman.core.usecase.ticket;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.permission.Permission;
import com.mossman.core.permission.PermissionEvaluator;
import com.mossman.core.repository.ProjectRepository;
import com.mossman.core.usecase.NotFoundException;

import java.util.Objects;
import java.util.UUID;

public final class UpdateTicketUseCase {

    private final ProjectRepository repository;

    public UpdateTicketUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /** Pass null for either field to leave it unchanged. */
    public Ticket execute(UUID actor, String projectId, UUID ticketId,
                          String newTitle, String newDescription) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ticketId, "ticketId");
        Project project = repository.find(projectId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
        PermissionEvaluator.require(project, actor, Permission.EDIT_TICKETS);

        Ticket original = project.findTicket(ticketId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.TICKET, ticketId.toString()));
        Ticket updated = original;
        if (newTitle != null) updated = updated.withTitle(newTitle);
        if (newDescription != null) updated = updated.withDescription(newDescription);
        repository.save(project.replaceTicket(updated));
        return updated;
    }
}
