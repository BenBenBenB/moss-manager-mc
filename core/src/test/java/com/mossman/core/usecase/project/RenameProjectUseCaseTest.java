package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.PermissionDeniedException;
import com.mossman.core.usecase.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenameProjectUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final RenameProjectUseCase usecase = new RenameProjectUseCase(repo);

    @Test
    void ownerCanRename() {
        repo.save(Projects.seeded(Players.OWNER));
        Project after = usecase.execute(Players.OWNER, "test", "Renamed");
        assertEquals("Renamed", after.name());
        assertEquals("Renamed", repo.find("test").orElseThrow().name());
    }

    @Test
    void memberWithEditProjectCanRename() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Admin");
        repo.save(p);
        Project after = usecase.execute(Players.ALICE, "test", "Renamed");
        assertEquals("Renamed", after.name());
    }

    @Test
    void memberWithoutEditProjectDenied() {
        Project p = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(p);
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", "Renamed"));
    }

    @Test
    void ownerStrippedOfAdminCannotRename() {
        Project p = Projects.unassignAll(Projects.seeded(Players.OWNER), Players.OWNER);
        repo.save(p);
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.OWNER, "test", "Renamed"));
    }

    @Test
    void blankNameRejected() {
        repo.save(Projects.seeded(Players.OWNER));
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "test", "   "));
    }

    @Test
    void missingProjectThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> usecase.execute(Players.OWNER, "absent", "x"));
    }
}
