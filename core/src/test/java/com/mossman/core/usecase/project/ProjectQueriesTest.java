package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProjectQueriesTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final ProjectQueries queries = new ProjectQueries(repo);

    @Test
    void getProjectReturnsEmptyWhenNonexistent() {
        assertTrue(queries.getProject(Players.OWNER, "absent").isEmpty());
    }

    @Test
    void getProjectReturnsForOwner() {
        repo.save(Projects.seeded(Players.OWNER));
        assertTrue(queries.getProject(Players.OWNER, "test").isPresent());
    }

    @Test
    void getProjectThrowsForUnauthorizedNonMember() {
        Project p = Projects.allowingNonMembers(Projects.seeded(Players.OWNER), false);
        repo.save(p);
        assertThrows(PermissionDeniedException.class,
                () -> queries.getProject(Players.STRANGER, "test"));
    }

    @Test
    void getProjectReturnsForNonMemberWhenAllowed() {
        repo.save(Projects.seeded(Players.OWNER));
        Optional<Project> seen = queries.getProject(Players.STRANGER, "test");
        assertTrue(seen.isPresent());
    }

    @Test
    void listProjectsFiltersByViewPermission() {
        Project visible = Projects.seeded("a", "A", Players.OWNER);
        Project hidden = Projects.allowingNonMembers(Projects.seeded("b", "B", Players.OWNER), false);
        repo.save(visible);
        repo.save(hidden);

        List<Project> alice = queries.listProjects(Players.ALICE);
        assertEquals(1, alice.size());
        assertEquals("a", alice.get(0).id());

        // Stripping Alice's roles in hidden but allowing non-members doesn't change visibility through default.
        // Owner sees both via their Admin assignment.
        List<Project> owner = queries.listProjects(Players.OWNER);
        assertEquals(2, owner.size());
    }
}
