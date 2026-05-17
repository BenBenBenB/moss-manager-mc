package com.mossman.persistence;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectJsonTest {

    private final ProjectJson json = new ProjectJson();

    @Test
    void roundTripsAFreshProject() {
        Project original = Project.create("p1", "Demo", UUID.randomUUID());
        Project decoded = json.fromJson(json.toJson(original));
        assertEquals(original, decoded);
    }

    @Test
    void roundTripsAProjectWithATicket() {
        Project original = Project.create("p2", "With Ticket", UUID.randomUUID());
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                1,
                "Fix the thing",
                "longer description",
                UUID.randomUUID(),
                original.statuses().get(0).id(),
                original.types().get(0).id(),
                1_700_000_000_000L);
        original = original.addTicket(ticket);

        String encoded = json.toJson(original);
        Project decoded = json.fromJson(encoded);

        assertEquals(original, decoded);
    }
}
