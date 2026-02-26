package com.mossman.domain.usecases;

import com.mossman.domain.entities.Ticket;
import com.mossman.domain.repositories.TicketRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnobserveTicketUseCase {
    private final TicketRepository ticketRepository;

    public UnobserveTicketUseCase(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket execute(long ticketId, UUID requesterId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + ticketId));

        if (!ticket.getObservers().contains(requesterId)) {
            throw new IllegalArgumentException("Player is not observing this ticket");
        }

        List<UUID> newObservers = new ArrayList<>(ticket.getObservers());
        newObservers.remove(requesterId);

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
