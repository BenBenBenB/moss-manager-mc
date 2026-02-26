package com.mossman.domain.usecases;

import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketUnassignedEvent;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnassignTicketUseCase {
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public UnassignTicketUseCase(TicketRepository ticketRepository, ProjectRepository projectRepository, DomainEventBus eventBus) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    /**
     * @param ticketId    DB id of the ticket
     * @param requesterId UUID of the user requesting the change (must be EDITOR or above)
     * @param assigneeId  UUID of the player to unassign
     * @return the updated Ticket
     * @throws IllegalArgumentException if ticket/project not found or player is not assigned
     * @throws SecurityException        if requester lacks EDITOR permission
     */
    public Ticket execute(long ticketId, UUID requesterId, UUID assigneeId) {
        Ticket current = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + ticketId));

        var project = projectRepository.findById(current.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + current.getProjectId()));

        PermissionChecker.requireProjectEditor(project, requesterId);

        if (!current.getAssignees().contains(assigneeId)) {
            throw new IllegalArgumentException("Player is not assigned to this ticket");
        }

        List<UUID> newAssignees = new ArrayList<>(current.getAssignees());
        newAssignees.remove(assigneeId);

        Ticket updated = new Ticket(
                current.getId(), current.getProjectId(), current.getTicketNumber(),
                current.getTitle(), current.getDescription(), current.getType(),
                current.getStatus(), current.getPriority(),
                newAssignees, current.getObservers(),
                current.getCreator(), current.getLabels(),
                current.getCreatedAt(), System.currentTimeMillis(), current.getSprintId()
        );

        Ticket saved = ticketRepository.save(updated);
        eventBus.publish(new TicketUnassignedEvent(saved, assigneeId, Instant.now()));
        return saved;
    }
}
