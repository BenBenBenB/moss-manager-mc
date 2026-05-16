package com.mossman.core.usecase.type;

import com.mossman.core.model.Project;
import com.mossman.core.model.Ticket;
import com.mossman.core.model.TicketType;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import com.mossman.core.usecase.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TypeUseCasesTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();

    @Test
    void createAppendsType() {
        UUID newId = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
        repo.save(Projects.seeded(Players.OWNER));
        TicketType t = new CreateTypeUseCase(repo, () -> newId)
                .execute(Players.OWNER, "test", "Spike", 0xFFFFFF, 0xAA00AA);
        Project p = repo.find("test").orElseThrow();
        assertEquals(newId, t.id());
        assertEquals("Spike", p.types().get(p.types().size() - 1).name());
    }

    @Test
    void editorWithoutEditProjectCannotCreate() {
        repo.save(Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor"));
        assertThrows(PermissionDeniedException.class,
                () -> new CreateTypeUseCase(repo).execute(Players.ALICE, "test", "X", 0, 0));
    }

    @Test
    void updateModifiesNameAndColors() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID typeId = base.types().get(0).id();
        new UpdateTypeUseCase(repo).execute(Players.OWNER, "test", typeId, "Renamed", 0x111, 0x222);
        TicketType t = repo.find("test").orElseThrow().findType(typeId).orElseThrow();
        assertEquals("Renamed", t.name());
        assertEquals(0x111, t.textColor());
    }

    @Test
    void updateUnknownTypeThrowsNotFound() {
        repo.save(Projects.seeded(Players.OWNER));
        assertThrows(NotFoundException.class,
                () -> new UpdateTypeUseCase(repo).execute(Players.OWNER, "test", UUID.randomUUID(), "X", 0, 0));
    }

    @Test
    void deleteRequiresReplacementAndCascadesTickets() {
        Project base = Projects.seeded(Players.OWNER);
        UUID typeToDelete = base.types().get(0).id();
        UUID replacement  = base.types().get(1).id();
        UUID statusId     = base.statuses().get(0).id();
        Ticket t = new Ticket(UUID.randomUUID(), "T", "", null, statusId, typeToDelete, 0L);
        repo.save(base.addTicket(t));

        new DeleteTypeUseCase(repo).execute(Players.OWNER, "test", typeToDelete, replacement);
        Project after = repo.find("test").orElseThrow();
        assertTrue(after.findType(typeToDelete).isEmpty());
        assertEquals(replacement, after.findTicket(t.id()).orElseThrow().typeId());
    }

    @Test
    void deleteRejectsIdenticalReplacement() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID typeId = base.types().get(0).id();
        assertThrows(ValidationException.class,
                () -> new DeleteTypeUseCase(repo).execute(Players.OWNER, "test", typeId, typeId));
    }

    @Test
    void deleteRejectsLastRemainingType() {
        Project base = Projects.seeded(Players.OWNER);
        UUID keep = base.types().get(0).id();
        UUID a    = base.types().get(1).id();
        UUID b    = base.types().get(2).id();
        UUID c    = base.types().get(3).id();
        repo.save(base);
        new DeleteTypeUseCase(repo).execute(Players.OWNER, "test", a, keep);
        new DeleteTypeUseCase(repo).execute(Players.OWNER, "test", b, keep);
        new DeleteTypeUseCase(repo).execute(Players.OWNER, "test", c, keep);
        assertThrows(ValidationException.class,
                () -> new DeleteTypeUseCase(repo).execute(Players.OWNER, "test", keep, UUID.randomUUID()));
    }

    @Test
    void reorderRequiresExactPermutation() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        List<UUID> reversed = List.of(
                base.types().get(3).id(),
                base.types().get(2).id(),
                base.types().get(1).id(),
                base.types().get(0).id());
        Project after = new ReorderTypesUseCase(repo)
                .execute(Players.OWNER, "test", reversed);
        for (int i = 0; i < 4; i++) {
            assertEquals(reversed.get(i), after.types().get(i).id());
        }
    }
}
