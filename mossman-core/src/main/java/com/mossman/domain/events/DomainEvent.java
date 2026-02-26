package com.mossman.domain.events;

import java.time.Instant;

public interface DomainEvent {
    Instant getOccurredAt();
}
