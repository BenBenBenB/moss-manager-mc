package com.mossman.domain.events;

import com.mossman.domain.entities.Ticket;

import java.time.Instant;
import java.util.UUID;

public record TicketUnassignedEvent(Ticket ticket, UUID assigneeId, Instant occurredAt) implements DomainEvent {
    @Override
    public Instant getOccurredAt() { return occurredAt; }
}
