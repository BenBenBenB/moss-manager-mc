package com.mossman.domain.events;

import com.mossman.domain.entities.Project;
import java.time.Instant;

public record ProjectCreatedEvent(Project project, Instant occurredAt) implements DomainEvent {
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
