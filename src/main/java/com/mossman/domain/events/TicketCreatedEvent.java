package com.mossman.domain.events;

import com.mossman.domain.entities.Ticket;
import java.time.Instant;

public record TicketCreatedEvent(Ticket ticket, Instant occurredAt) implements DomainEvent {
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
