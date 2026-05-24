package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.permission.Permission;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UpdateRoleGrantsUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final UpdateRoleGrantsUseCase usecase = new UpdateRoleGrantsUseCase(repo);

    @Test
    void adminCanUpdateRegularRoleGrants() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID editorId = Projects.roleIdByName(base, "Editor");
        Set<Permission> next = Set.of(Permission.VIEW_PROJECT);
        usecase.execute(Players.OWNER, "test", editorId, next);
        assertEquals(next, repo.find("test").orElseThrow().findRole(editorId).orElseThrow().grants());
    }

    @Test
    void onlyOwnerCanUpdateDefaultRoleGrants() {
        Project base = Projects.assignByName(Projects.seeded(Players.OWNER), Players.ALICE, "Admin");
        repo.save(base);
        UUID def = base.defaultRoleId();
        assertThrows(PermissionDeniedException.class,
                () -> usecase.execute(Players.ALICE, "test", def, Set.of()));
        usecase.execute(Players.OWNER, "test", def, Set.of(Permission.VIEW_PROJECT));
        assertEquals(Set.of(Permission.VIEW_PROJECT),
                repo.find("test").orElseThrow().findRole(def).orElseThrow().grants());
    }
}
