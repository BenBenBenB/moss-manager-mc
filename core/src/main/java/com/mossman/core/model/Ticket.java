package com.mossman.core.model;

import java.util.Objects;
import java.util.UUID;

public record Ticket(
        UUID id,
        String title,
        String description,
        UUID assigneeUuid,
        UUID statusId,
        UUID typeId,
        long createdAt
) {

    public Ticket {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(statusId, "statusId");
        Objects.requireNonNull(typeId, "typeId");
        if (title.isBlank()) throw new IllegalArgumentException("title is blank");
        description = description == null ? "" : description;
    }

    public Ticket withTitle(String newTitle) {
        return new Ticket(id, newTitle, description, assigneeUuid, statusId, typeId, createdAt);
    }

    public Ticket withDescription(String newDescription) {
        return new Ticket(id, title, newDescription, assigneeUuid, statusId, typeId, createdAt);
    }

    public Ticket withAssignee(UUID newAssignee) {
        return new Ticket(id, title, description, newAssignee, statusId, typeId, createdAt);
    }

    public Ticket withStatus(UUID newStatusId) {
        return new Ticket(id, title, description, assigneeUuid, newStatusId, typeId, createdAt);
    }

    public Ticket withType(UUID newTypeId) {
        return new Ticket(id, title, description, assigneeUuid, statusId, newTypeId, createdAt);
    }
}
