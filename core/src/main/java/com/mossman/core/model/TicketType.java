package com.mossman.core.model;

import java.util.Objects;
import java.util.UUID;

public record TicketType(UUID id, String name, int textColor, int backgroundColor) {

    public TicketType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("name is blank");
    }

    public TicketType withName(String newName) {
        return new TicketType(id, newName, textColor, backgroundColor);
    }

    public TicketType withColors(int newTextColor, int newBackgroundColor) {
        return new TicketType(id, name, newTextColor, newBackgroundColor);
    }
}
