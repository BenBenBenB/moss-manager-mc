package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import com.mossman.core.usecase.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssignRoleUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final AssignRoleUseCase usecase = new AssignRoleUseCase(repo);

    @Test
    void adminCanAssignRegularRole() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        usecase.execute(Players.OWNER, "test", Players.ALICE, editorId);
        assertEquals(Set.of(editorId), repo.find("test").orElseThrow().memberRoles().get(Players.ALICE));
    }

    @Test
    void cannotAssignDefaultRole() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "test", Players.ALICE, base.defaultRoleId()));
    }

    @Test
    void unknownRoleThrowsNotFound() {
        repo.save(Projects.seeded(Players.OWNER));
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "test", Players.ALICE, UUID.randomUUID()));
    }

    @Test
    void assignIsIdempotent() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        Project after = usecase.execute(Players.OWNER, "test", Players.ALICE, editorId);
        assertEquals(Set.of(editorId), after.memberRoles().get(Players.ALICE));
    }

    @Test
    void editorCannotAssign() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(base);
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", Players.BOB, viewerId));
    }
}
