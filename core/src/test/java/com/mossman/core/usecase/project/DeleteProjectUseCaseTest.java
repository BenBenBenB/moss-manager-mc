package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeleteProjectUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final DeleteProjectUseCase usecase = new DeleteProjectUseCase(repo);

    @Test
    void ownerCanDelete() {
        repo.save(Projects.seeded(Players.OWNER));
        usecase.execute(Players.OWNER, "test");
        assertEquals(0, repo.size());
    }

    @Test
    void nonOwnerWithAdminPermissionsCannotDelete() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Admin");
        repo.save(p);
        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test"));
        assertTrue(ex.isOwnerOnly());
        assertEquals(1, repo.size());
    }

    @Test
    void strangerCannotDelete() {
        repo.save(Projects.seeded(Players.OWNER));
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.STRANGER, "test"));
        assertEquals(1, repo.size());
    }

    @Test
    void missingProjectThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "absent"));
    }
}
