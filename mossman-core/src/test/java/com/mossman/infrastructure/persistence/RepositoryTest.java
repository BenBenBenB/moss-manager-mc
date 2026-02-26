package com.mossman.infrastructure.persistence;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {
    private DatabaseManager databaseManager;
    private OrmLiteProjectRepository projectRepository;
    private OrmLiteTicketRepository ticketRepository;

    @BeforeEach
    void setUp() throws SQLException {
        // Use in-memory SQLite database for testing
        databaseManager = new DatabaseManager("jdbc:sqlite::memory:");
        var memberRepository = new OrmLiteMemberRepository(databaseManager.getMemberDao());
        projectRepository = new OrmLiteProjectRepository(databaseManager.getProjectDao(), memberRepository);
        ticketRepository = new OrmLiteTicketRepository(databaseManager.getTicketDao());
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseManager.close();
    }

    @Test
    void testSaveAndFindProject() {
        Project project = Project.builder()
                .name("MossMan")
                .ticketPrefix("MOSS")
                .members(java.util.List.of(new Member(0, UUID.randomUUID(), "Owner", "Owner", Permission.OWNER)))
                .externalUserPermission(Permission.ADMIN)
                .build();

        Project saved = projectRepository.save(project);
        assertTrue(saved.getId() > 0);
        assertEquals("MossMan", saved.getName());

        Optional<Project> found = projectRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("MOSS", found.get().getTicketPrefix());
    }

    @Test
    void testSaveAndFindTicket() {
        Project project = Project.builder()
                .name("MossMan")
                .ticketPrefix("MOSS")
                .members(java.util.List.of(new Member(0, UUID.randomUUID(), "Owner", "Owner", Permission.OWNER)))
                .build();
        Project savedProject = projectRepository.save(project);

        Ticket ticket = new Ticket(0, savedProject.getId(), 1, "Fix bugs", "Description", 
                "BUG", "OPEN", Priority.HIGH, Collections.emptyList(), Collections.emptyList(), 
                UUID.randomUUID(), Collections.emptyList(), System.currentTimeMillis(), 
                System.currentTimeMillis(), null);

        Ticket savedTicket = ticketRepository.save(ticket);
        assertTrue(savedTicket.getId() > 0);
        assertEquals(savedProject.getId(), savedTicket.getProjectId());

        Optional<Ticket> found = ticketRepository.findById(savedTicket.getId());
        assertTrue(found.isPresent());
        assertEquals("Fix bugs", found.get().getTitle());
    }
}
