package com.mossman.domain.events;

import com.mossman.domain.entities.Comment;
import com.mossman.domain.entities.Ticket;

import java.time.Instant;
import java.util.UUID;

public record TicketCommentedEvent(Comment comment, Ticket ticket, UUID requesterId, Instant occurredAt) implements DomainEvent {
    @Override
    public Instant getOccurredAt() { return occurredAt; }
}
