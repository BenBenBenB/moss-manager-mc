package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UpdateRoleColorUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final UpdateRoleColorUseCase usecase = new UpdateRoleColorUseCase(repo);

    @Test
    void adminCanUpdateRegularRoleColor() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        usecase.execute(Players.OWNER, "test", viewerId, 0x123456);
        assertEquals(0x123456, repo.find("test").orElseThrow().findRole(viewerId).orElseThrow().color());
    }

    @Test
    void editorCannotChangeColor() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(base);
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", viewerId, 0));
    }
}
