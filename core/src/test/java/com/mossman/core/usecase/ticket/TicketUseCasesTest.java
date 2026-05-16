package com.mossman.core.usecase.ticket;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.support.FixedClock;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketUseCasesTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();

    private Project newProject() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(p);
        return p;
    }

    private Ticket seedTicket(Project p, UUID assignee) {
        Ticket t = new Ticket(UUID.randomUUID(), "T", "", assignee,
                p.statuses().get(0).id(), p.types().get(0).id(), 0L);
        repo.save(p.addTicket(t));
        return t;
    }

    // ---- Create ----

    @Test
    void createWithEditorAssignsCorrectFields() {
        Project p = newProject();
        UUID newId = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
        Ticket t = new CreateTicketUseCase(repo, new FixedClock(1234L), () -> newId)
                .execute(Players.ALICE, "test", "Title", "Desc", null,
                        p.statuses().get(0).id(), p.types().get(0).id());
        assertEquals(newId, t.id());
        assertEquals("Title", t.title());
        assertEquals(1234L, t.createdAt());
        assertEquals(1, repo.find("test").orElseThrow().tickets().size());
    }

    @Test
    void createRejectedWithoutCreateTickets() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.STRANGER, "Viewer");
        repo.save(p);
        assertThrows(PermissionDeniedException.class,
                () -> new CreateTicketUseCase(repo).execute(Players.STRANGER, "test", "x", "", null,
                        p.statuses().get(0).id(), p.types().get(0).id()));
    }

    @Test
    void createRejectsUnknownStatusOrType() {
        Project p = newProject();
        assertThrows(NotFoundException.class,
                () -> new CreateTicketUseCase(repo).execute(Players.ALICE, "test", "x", "", null,
                        UUID.randomUUID(), p.types().get(0).id()));
        assertThrows(NotFoundException.class,
                () -> new CreateTicketUseCase(repo).execute(Players.ALICE, "test", "x", "", null,
                        p.statuses().get(0).id(), UUID.randomUUID()));
    }

    // ---- Update ----

    @Test
    void updatePartialFields() {
        Project p = newProject();
        Ticket t = seedTicket(p, null);
        Ticket after = new UpdateTicketUseCase(repo)
                .execute(Players.ALICE, "test", t.id(), "New Title", null);
        assertEquals("New Title", after.title());
        assertEquals("", after.description());
    }

    @Test
    void updateDeniedForViewer() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.STRANGER, "Viewer");
        Ticket t = new Ticket(UUID.randomUUID(), "T", "", null,
                p.statuses().get(0).id(), p.types().get(0).id(), 0L);
        repo.save(p.addTicket(t));
        assertThrows(PermissionDeniedException.class,
                () -> new UpdateTicketUseCase(repo).execute(Players.STRANGER, "test", t.id(), "x", null));
    }

    // ---- Status / type changes ----

    @Test
    void changeStatusByEditor() {
        Project p = newProject();
        Ticket t = seedTicket(p, null);
        UUID newStatusId = p.statuses().get(1).id();
        Ticket after = new ChangeTicketStatusUseCase(repo)
                .execute(Players.ALICE, "test", t.id(), newStatusId);
        assertEquals(newStatusId, after.statusId());
    }

    @Test
    void changeStatusRejectsUnknownStatusId() {
        Project p = newProject();
        Ticket t = seedTicket(p, null);
        assertThrows(NotFoundException.class,
                () -> new ChangeTicketStatusUseCase(repo)
                        .execute(Players.ALICE, "test", t.id(), UUID.randomUUID()));
    }

    @Test
    void changeTypeByEditor() {
        Project p = newProject();
        Ticket t = seedTicket(p, null);
        UUID newTypeId = p.types().get(1).id();
        Ticket after = new ChangeTicketTypeUseCase(repo)
                .execute(Players.ALICE, "test", t.id(), newTypeId);
        assertEquals(newTypeId, after.typeId());
    }

    // ---- Assign ----

    @Test
    void assignAndUnassign() {
        Project p = newProject();
        Ticket t = seedTicket(p, null);
        Ticket assigned = new AssignTicketUseCase(repo)
                .execute(Players.ALICE, "test", t.id(), Players.BOB);
        assertEquals(Players.BOB, assigned.assigneeUuid());
        Ticket unassigned = new AssignTicketUseCase(repo)
                .execute(Players.ALICE, "test", t.id(), null);
        assertNull(unassigned.assigneeUuid());
    }

    // ---- Delete ----

    @Test
    void deleteRequiresDeleteTickets() {
        Project p = newProject();
        Ticket t = seedTicket(p, null);
        // Editor lacks DELETE_TICKETS.
        assertThrows(PermissionDeniedException.class,
                () -> new DeleteTicketUseCase(repo).execute(Players.ALICE, "test", t.id()));
        // Owner has Admin, which includes DELETE_TICKETS.
        new DeleteTicketUseCase(repo).execute(Players.OWNER, "test", t.id());
        assertTrue(repo.find("test").orElseThrow().tickets().isEmpty());
    }

    @Test
    void deleteUnknownTicketThrowsNotFound() {
        newProject();
        assertThrows(NotFoundException.class,
                () -> new DeleteTicketUseCase(repo).execute(Players.OWNER, "test", UUID.randomUUID()));
    }
}
