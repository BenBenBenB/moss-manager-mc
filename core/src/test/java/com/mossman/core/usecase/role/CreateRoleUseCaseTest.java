package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.permission.Permission;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateRoleUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final UUID newRoleId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private final CreateRoleUseCase usecase = new CreateRoleUseCase(repo, () -> newRoleId);

    @Test
    void ownerCreatesRoleAppendedAboveDefault() {
        repo.save(Projects.seeded(Players.OWNER));
        Role created = usecase.execute(Players.OWNER, "test", "Triager",
                Set.of(Permission.VIEW_PROJECT, Permission.CHANGE_TICKET_STATUS), 0xAABBCC);
        Project p = repo.find("test").orElseThrow();
        assertEquals(newRoleId, created.id());
        assertEquals("Triager", created.name());
        // Default is still last; new role is second-to-last.
        assertEquals(newRoleId, p.roles().get(p.roles().size() - 2).id());
        assertEquals(p.defaultRoleId(), p.roles().get(p.roles().size() - 1).id());
    }

    @Test
    void memberWithoutManageRolesDenied() {
        repo.save(Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor"));
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", "X", Set.of(), 0));
    }

    @Test
    void missingProjectThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "absent", "X", Set.of(), 0));
    }
}
