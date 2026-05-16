package com.mossman.core.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketStatusTypeTest {

    @Test
    void statusRejectsNullAndBlank() {
        assertThrows(NullPointerException.class, () -> new TicketStatus(null, "n", 0, 0));
        assertThrows(NullPointerException.class, () -> new TicketStatus(UUID.randomUUID(), null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TicketStatus(UUID.randomUUID(), "", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TicketStatus(UUID.randomUUID(), "  ", 0, 0));
    }

    @Test
    void typeRejectsNullAndBlank() {
        assertThrows(NullPointerException.class, () -> new TicketType(null, "n", 0, 0));
        assertThrows(NullPointerException.class, () -> new TicketType(UUID.randomUUID(), null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TicketType(UUID.randomUUID(), "", 0, 0));
    }

    @Test
    void withMethodsProduceNewInstance() {
        UUID id = UUID.randomUUID();
        TicketStatus s = new TicketStatus(id, "Old", 0x111, 0x222);
        assertEquals("New", s.withName("New").name());
        TicketStatus colored = s.withColors(0xAAA, 0xBBB);
        assertEquals(0xAAA, colored.textColor());
        assertEquals(0xBBB, colored.backgroundColor());

        TicketType t = new TicketType(id, "Old", 0x111, 0x222);
        assertEquals("New", t.withName("New").name());
    }
}
