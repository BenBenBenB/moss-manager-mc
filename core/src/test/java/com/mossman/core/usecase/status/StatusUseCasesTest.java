package com.mossman.core.usecase.status;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketStatus;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import com.mossman.core.usecase.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatusUseCasesTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();

    @Test
    void createAppendsStatus() {
        UUID newId = UUID.fromString("00000000-0000-0000-0000-00000000005a");
        repo.save(Projects.seeded(Players.OWNER));
        TicketStatus s = new CreateStatusUseCase(repo, () -> newId)
                .execute(Players.OWNER, "test", "Blocked", 0xFFFFFF, 0xFFA500);
        Project p = repo.find("test").orElseThrow();
        assertEquals(newId, s.id());
        assertEquals("Blocked", p.statuses().get(p.statuses().size() - 1).name());
    }

    @Test
    void editorWithoutEditProjectCannotCreate() {
        repo.save(Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor"));
        assertThrows(PermissionDeniedException.class,
                () -> new CreateStatusUseCase(repo).execute(Players.ALICE, "test", "X", 0, 0));
    }

    @Test
    void updateModifiesNameAndColors() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID statusId = base.statuses().get(0).id();
        new UpdateStatusUseCase(repo).execute(Players.OWNER, "test", statusId, "Renamed", 0x111, 0x222);
        TicketStatus s = repo.find("test").orElseThrow().findStatus(statusId).orElseThrow();
        assertEquals("Renamed", s.name());
        assertEquals(0x111, s.textColor());
        assertEquals(0x222, s.backgroundColor());
    }

    @Test
    void deleteRequiresReplacementAndCascadesTickets() {
        Project base = Projects.seeded(Players.OWNER);
        UUID statusToDelete = base.statuses().get(0).id();
        UUID replacement    = base.statuses().get(1).id();
        UUID typeId         = base.types().get(0).id();
        Ticket t = new Ticket(UUID.randomUUID(), 1, "T", "", null, statusToDelete, typeId, 0L);
        repo.save(base.addTicket(t));

        new DeleteStatusUseCase(repo).execute(Players.OWNER, "test", statusToDelete, replacement);
        Project after = repo.find("test").orElseThrow();
        assertTrue(after.findStatus(statusToDelete).isEmpty());
        assertEquals(replacement, after.findTicket(t.id()).orElseThrow().statusId());
    }

    @Test
    void deleteRejectsIdenticalReplacement() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID statusId = base.statuses().get(0).id();
        assertThrows(ValidationException.class,
                () -> new DeleteStatusUseCase(repo).execute(Players.OWNER, "test", statusId, statusId));
    }

    @Test
    void deleteRejectsNonexistentReplacement() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID statusId = base.statuses().get(0).id();
        assertThrows(NotFoundException.class,
                () -> new DeleteStatusUseCase(repo).execute(Players.OWNER, "test", statusId, UUID.randomUUID()));
    }

    @Test
    void deleteRejectsLastRemainingStatus() {
        Project base = Projects.seeded(Players.OWNER);
        UUID keep = base.statuses().get(0).id();
        UUID a    = base.statuses().get(1).id();
        UUID b    = base.statuses().get(2).id();
        repo.save(base);
        new DeleteStatusUseCase(repo).execute(Players.OWNER, "test", a, keep);
        new DeleteStatusUseCase(repo).execute(Players.OWNER, "test", b, keep);
        assertThrows(ValidationException.class,
                () -> new DeleteStatusUseCase(repo).execute(Players.OWNER, "test", keep, UUID.randomUUID()));
    }

    @Test
    void reorderRequiresExactPermutation() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID a = base.statuses().get(0).id();
        UUID b = base.statuses().get(1).id();
        UUID c = base.statuses().get(2).id();
        Project after = new ReorderStatusesUseCase(repo)
                .execute(Players.OWNER, "test", List.of(c, b, a));
        assertEquals(List.of(c, b, a), List.of(after.statuses().get(0).id(),
                                                after.statuses().get(1).id(),
                                                after.statuses().get(2).id()));
    }

    @Test
    void reorderRejectsBadList() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID a = base.statuses().get(0).id();
        UUID b = base.statuses().get(1).id();
        List<UUID> partial = new ArrayList<>(List.of(a, b));
        assertThrows(ValidationException.class,
                () -> new ReorderStatusesUseCase(repo).execute(Players.OWNER, "test", partial));
    }
}
