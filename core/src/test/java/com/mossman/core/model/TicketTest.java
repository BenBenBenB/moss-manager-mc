package com.mossman.core.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketTest {

    private final UUID id = UUID.randomUUID();
    private final UUID statusId = UUID.randomUUID();
    private final UUID typeId = UUID.randomUUID();

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(NullPointerException.class,
                () -> new Ticket(null, 1,"t", "d", null, statusId, typeId, 0L));
        assertThrows(NullPointerException.class,
                () -> new Ticket(id, 1,null, "d", null, statusId, typeId, 0L));
        assertThrows(NullPointerException.class,
                () -> new Ticket(id, 1,"t", "d", null, null, typeId, 0L));
        assertThrows(NullPointerException.class,
                () -> new Ticket(id, 1,"t", "d", null, statusId, null, 0L));
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket(id, 1,"", "d", null, statusId, typeId, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket(id, 1,"   ", "d", null, statusId, typeId, 0L));
    }

    @Test
    void nullDescriptionNormalizesToEmpty() {
        Ticket t = new Ticket(id, 1,"t", null, null, statusId, typeId, 0L);
        assertEquals("", t.description());
    }

    @Test
    void assigneeIsNullable() {
        Ticket t = new Ticket(id, 1,"t", "d", null, statusId, typeId, 0L);
        assertNull(t.assigneeUuid());
    }

    @Test
    void withMethodsPreserveOtherFields() {
        Ticket t = new Ticket(id, 1,"old", "desc", null, statusId, typeId, 100L);
        UUID newStatus = UUID.randomUUID();
        UUID newType = UUID.randomUUID();
        UUID newAssignee = UUID.randomUUID();

        Ticket renamed = t.withTitle("new");
        assertEquals("new", renamed.title());
        assertEquals(t.description(), renamed.description());
        assertEquals(t.statusId(), renamed.statusId());
        assertEquals(t.createdAt(), renamed.createdAt());

        assertEquals(newStatus, t.withStatus(newStatus).statusId());
        assertEquals(newType, t.withType(newType).typeId());
        assertEquals(newAssignee, t.withAssignee(newAssignee).assigneeUuid());
        assertEquals("updated", t.withDescription("updated").description());
    }
}
