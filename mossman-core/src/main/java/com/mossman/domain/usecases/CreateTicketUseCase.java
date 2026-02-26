package com.mossman.domain.usecases;

import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketCreatedEvent;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.validation.EntityValidator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateTicketUseCase {
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public CreateTicketUseCase(TicketRepository ticketRepository, ProjectRepository projectRepository, DomainEventBus eventBus) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    public Ticket execute(Ticket ticket, java.util.UUID requesterId) {
        com.mossman.domain.entities.Project project = projectRepository.findById(ticket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + ticket.getProjectId()));

        Permission perm = com.mossman.domain.auth.PermissionChecker.getEffectivePermission(project, requesterId);
        if (perm == Permission.FORBID || perm == Permission.VIEWER) {
            throw new SecurityException("Insufficient permission: CREATOR required");
        }

        EntityValidator.requireValidTicketTitle(ticket.getTitle());
        EntityValidator.requireValidTicketDescription(ticket.getDescription());

        List<UUID> initialObservers = new ArrayList<>(ticket.getObservers());
        if (!initialObservers.contains(ticket.getCreator())) {
            initialObservers.add(ticket.getCreator());
        }
        Ticket ticketWithObserver = new Ticket(
                ticket.getId(), ticket.getProjectId(), ticket.getTicketNumber(),
                ticket.getTitle(), ticket.getDescription(), ticket.getType(),
                ticket.getStatus(), ticket.getPriority(), ticket.getAssignees(),
                initialObservers, ticket.getCreator(), ticket.getLabels(),
                ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getSprintId()
        );

        Ticket savedTicket = ticketRepository.save(ticketWithObserver);
        eventBus.publish(new TicketCreatedEvent(savedTicket, Instant.now()));
        return savedTicket;
    }
}
