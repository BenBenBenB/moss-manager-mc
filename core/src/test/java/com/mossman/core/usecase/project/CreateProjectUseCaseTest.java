package com.mossman.core.usecase.project;

import com.mossman.core.model.Project;
import com.mossman.core.support.InMemoryProjectRepository;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateProjectUseCaseTest {

    private final InMemoryProjectRepository repo = new InMemoryProjectRepository();
    private final CreateProjectUseCase usecase = new CreateProjectUseCase(repo);

    @Test
    void createsProjectAndAssignsCreatorToAdmin() {
        Project p = usecase.execute(Players.OWNER, "p1", "My Project");
        assertEquals("p1", p.id());
        assertEquals("My Project", p.name());
        assertEquals(Players.OWNER, p.ownerUuid());
        UUID adminId = Projects.roleIdByName(p, "Admin");
        assertEquals(Set.of(adminId), p.memberRoles().get(Players.OWNER));
        assertSame(p, repo.find("p1").orElseThrow());
    }

    @Test
    void seedsThreeStatusesAndFourTypes() {
        Project p = usecase.execute(Players.OWNER, "p1", "My Project");
        assertEquals(3, p.statuses().size());
        assertEquals(4, p.types().size());
    }

    @Test
    void seedsAdminEditorViewerAndDefault() {
        Project p = usecase.execute(Players.OWNER, "p1", "My Project");
        assertEquals(4, p.roles().size());
        assertEquals("Admin",    p.roles().get(0).name());
        assertEquals("Editor",   p.roles().get(1).name());
        assertEquals("Viewer",   p.roles().get(2).name());
        assertEquals("Everyone", p.roles().get(3).name());
        assertEquals(p.roles().get(3).id(), p.defaultRoleId());
    }

    @Test
    void rejectsDuplicateId() {
        usecase.execute(Players.OWNER, "p1", "Original");
        assertThrows(ValidationException.class,
                () -> usecase.execute(Players.OWNER, "p1", "Another"));
    }
}
