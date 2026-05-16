package com.mossman.core.usecase.role;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReorderRolesUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final ReorderRolesUseCase usecase = new ReorderRolesUseCase(repo);

    @Test
    void reordersAndKeepsDefaultLast() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID adminId  = Projects.roleIdByName(base, "Admin");
        UUID editorId = Projects.roleIdByName(base, "Editor");
        UUID viewerId = Projects.roleIdByName(base, "Viewer");

        Project after = usecase.execute(Players.OWNER, "test", List.of(viewerId, editorId, adminId));

        List<Role> roles = after.roles();
        assertEquals(viewerId, roles.get(0).id());
        assertEquals(editorId, roles.get(1).id());
        assertEquals(adminId,  roles.get(2).id());
        assertEquals(base.defaultRoleId(), roles.get(3).id());
    }

    @Test
    void rejectsOrderingThatIncludesDefault() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID adminId = Projects.roleIdByName(base, "Admin");
        UUID editorId = Projects.roleIdByName(base, "Editor");
        UUID viewerId = Projects.roleIdByName(base, "Viewer");
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "test",
                        List.of(adminId, editorId, viewerId, base.defaultRoleId())));
    }

    @Test
    void rejectsOrderingThatOmitsRole() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID adminId = Projects.roleIdByName(base, "Admin");
        UUID editorId = Projects.roleIdByName(base, "Editor");
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "test", List.of(adminId, editorId)));
    }

    @Test
    void rejectsOrderingWithDuplicates() {
        Project base = Projects.seeded(Players.OWNER);
        repo.save(base);
        UUID adminId = Projects.roleIdByName(base, "Admin");
        UUID editorId = Projects.roleIdByName(base, "Editor");
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "test", List.of(adminId, editorId, adminId)));
    }
}
