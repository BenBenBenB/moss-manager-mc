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

class DeleteRoleUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final DeleteRoleUseCase usecase = new DeleteRoleUseCase(repo);

    @Test
    void deletesRegularRoleAndCascadesMemberRolesCleanup() {
        Project base = Projects.seeded(Players.OWNER);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        Project withMember = Projects.assignByName(base, Players.ALICE, "Editor");
        // ALICE has only Editor; OWNER has Admin.
        repo.save(withMember);

        usecase.execute(Players.OWNER, "test", editorId);
        Project after = repo.find("test").orElseThrow();

        assertTrue(after.findRole(editorId).isEmpty());
        // ALICE's only role was Editor; she should be removed from memberRoles entirely.
        assertNull(after.memberRoles().get(Players.ALICE));
        // OWNER's Admin assignment is untouched.
        UUID adminId = Projects.roleIdByName(after, "Admin");
        assertEquals(Set.of(adminId), after.memberRoles().get(Players.OWNER));
    }

    @Test
    void deletingRoleRetainsOtherAssignments() {
        Project base = Projects.seeded(Players.OWNER);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        Project withMember = Projects.assignByName(
                Projects.assignByName(base, Players.ALICE, "Viewer"),
                Players.ALICE, "Editor");
        repo.save(withMember);

        usecase.execute(Players.OWNER, "test", editorId);

        Set<UUID> aliceRoles = repo.find("test").orElseThrow().memberRoles().get(Players.ALICE);
        assertEquals(Set.of(viewerId), aliceRoles);
    }

    @Test
    void cannotDeleteDefaultRole() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "test", base.defaultRoleId()));
    }

    @Test
    void unknownRoleThrowsNotFound() {
        repo.save(Projects.seeded(Players.OWNER));
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "test", UUID.randomUUID()));
    }

    @Test
    void editorCannotDelete() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(base);
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", viewerId));
    }
}
