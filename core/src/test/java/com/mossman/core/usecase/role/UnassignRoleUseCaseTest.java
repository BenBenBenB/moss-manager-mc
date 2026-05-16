package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnassignRoleUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final UnassignRoleUseCase usecase = new UnassignRoleUseCase(repo);

    @Test
    void removingOnlyRoleEliminatesEntryFromMemberRoles() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        Project after = usecase.execute(Players.OWNER, "test", Players.ALICE, editorId);
        assertNull(after.memberRoles().get(Players.ALICE));
    }

    @Test
    void unassignIsIdempotent() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        Project after = usecase.execute(Players.OWNER, "test", Players.ALICE, editorId);
        assertSame(base, after);
    }

    @Test
    void removingOneOfManyKeepsTheOthers() {
        Project base = Projects.assignByName(
                Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Editor"),
                Players.ALICE, "Viewer");
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        Project after = usecase.execute(Players.OWNER, "test", Players.ALICE, editorId);
        assertEquals(Set.of(viewerId), after.memberRoles().get(Players.ALICE));
    }
}
