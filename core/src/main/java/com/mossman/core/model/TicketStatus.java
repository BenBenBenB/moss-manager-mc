package com.mossman.core.model;

import java.util.Objects;
import java.util.UUID;

public record TicketStatus(UUID id, String name, int textColor, int backgroundColor) {

    public TicketStatus {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("name is blank");
    }

    public TicketStatus withName(String newName) {
        return new TicketStatus(id, newName, textColor, backgroundColor);
    }

    public TicketStatus withColors(int newTextColor, int newBackgroundColor) {
        return new TicketStatus(id, name, newTextColor, newBackgroundColor);
    }
}
