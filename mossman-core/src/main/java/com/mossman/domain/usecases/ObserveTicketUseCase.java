package com.mossman.domain.usecases;

import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObserveTicketUseCase {
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;

    public ObserveTicketUseCase(TicketRepository ticketRepository, ProjectRepository projectRepository) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
    }

    public Ticket execute(long ticketId, UUID requesterId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + ticketId));

        var project = projectRepository.findById(ticket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + ticket.getProjectId()));

        Permission perm = PermissionChecker.getEffectivePermission(project, requesterId);
        if (perm == Permission.FORBID) {
            throw new SecurityException("Insufficient permission to observe this ticket");
        }

        if (ticket.getObservers().contains(requesterId)) {
            throw new IllegalArgumentException("Player is already observing this ticket");
        }

        List<UUID> newObservers = new ArrayList<>(ticket.getObservers());
        newObservers.add(requesterId);

        Ticket updated = new Ticket(
                ticket.getId(), ticket.getProjectId(), ticket.getTicketNumber(),
                ticket.getTitle(), ticket.getDescription(), ticket.getType(),
                ticket.getStatus(), ticket.getPriority(),
                ticket.getAssignees(), newObservers,
                ticket.getCreator(), ticket.getLabels(),
                ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getSprintId()
        );

        return ticketRepository.save(updated);
    }
}
