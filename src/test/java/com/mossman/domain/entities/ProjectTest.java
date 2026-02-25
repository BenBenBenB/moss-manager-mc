package com.mossman.domain.entities;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProjectTest {
    @Test
    void testProjectBuilder() {
        UUID ownerId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .name("Test Project")
                .ticketPrefix("TEST")
                .members(java.util.List.of(new Member(0, ownerId, "Owner", "Owner", Permission.OWNER)))
                .externalUserPermission(Permission.VIEWER)
                .build();

        assertEquals(1L, project.getId());
        assertEquals("Test Project", project.getName());
        assertEquals("TEST", project.getTicketPrefix());
        assertTrue(project.getOwner().isPresent());
        assertEquals(ownerId, project.getOwner().get().uuid());
        assertEquals(Permission.VIEWER, project.getExternalUserPermission());
        assertTrue(project.getStatuses().isEmpty());
    }
}
