package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetAllowNonMembersUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final SetAllowNonMembersUseCase usecase = new SetAllowNonMembersUseCase(repo);

    @Test
    void ownerCanToggle() {
        repo.save(Projects.seeded(Players.OWNER));
        Project after = usecase.execute(Players.OWNER, "test", false);
        assertFalse(after.allowNonMembers());
    }

    @Test
    void memberWithManageRolesCanToggle() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Admin");
        repo.save(p);
        Project after = usecase.execute(Players.ALICE, "test", false);
        assertFalse(after.allowNonMembers());
    }

    @Test
    void editorWithoutManageRolesIsDenied() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(p);
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", false));
    }

    @Test
    void ownerStrippedOfRolesIsDenied() {
        Project p = Projects.unassignAll(Projects.seeded(Players.OWNER), Players.OWNER);
        repo.save(p);
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.OWNER, "test", false));
    }

    @Test
    void missingProjectThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "absent", false));
    }
}
