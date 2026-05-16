package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RenameRoleUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final RenameRoleUseCase usecase = new RenameRoleUseCase(repo);

    @Test
    void ownerCanRenameRegularRole() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        usecase.execute(Players.OWNER, "test", editorId, "Coder");
        assertEquals("Coder", repo.find("test").orElseThrow().findRole(editorId).orElseThrow().name());
    }

    @Test
    void memberWithManageRolesCanRename() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Admin");
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        usecase.execute(Players.ALICE, "test", editorId, "Coder");
        assertEquals("Coder", repo.find("test").orElseThrow().findRole(editorId).orElseThrow().name());
    }

    @Test
    void editorCannotRenameAnyRole() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(base);
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", viewerId, "X"));
    }

    @Test
    void onlyOwnerCanRenameDefaultRole() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Admin");
        repo.save(base);
        UUID def = base.defaultRoleId();
        // Admin-but-not-owner is denied (owner-only).
        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", def, "Public"));
        assertTrue(ex.isOwnerOnly());
        // Owner succeeds.
        usecase.execute(Players.OWNER, "test", def, "Public");
        assertEquals("Public", repo.find("test").orElseThrow().findRole(def).orElseThrow().name());
    }

    @Test
    void unknownRoleThrowsNotFound() {
        repo.save(Projects.seeded(Players.OWNER));
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "test", UUID.randomUUID(), "X"));
    }
}
