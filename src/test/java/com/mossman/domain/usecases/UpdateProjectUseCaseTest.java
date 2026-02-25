package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateProjectUseCaseTest {

    private ProjectRepository projectRepository;
    private DomainEventBus eventBus;
    private UpdateProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(DomainEventBus.class);
        useCase = new UpdateProjectUseCase(projectRepository, eventBus);
    }

    @Test
    void testExecute_Success() {
        UUID adminId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, adminId, "Admin", "Admin", Permission.ADMIN)))
                .name("Old Name")
                .description("Old Desc")
                .ticketPrefix("OLD")
                .iconTexture("minecraft:stone")
                .textColor("white")
                .externalUserPermission(Permission.FORBID)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Map<String, String> patch = Map.of(
                "name", "New Name",
                "description", "New Desc",
                "iconTexture", "minecraft:dirt",
                "textColor", "red"
        );

        Project updatedProject = useCase.execute(1L, adminId, patch);

        assertNotNull(updatedProject);
        assertEquals("New Name", updatedProject.getName());
        assertEquals("New Desc", updatedProject.getDescription());
        assertEquals("minecraft:dirt", updatedProject.getIconTexture());
        assertEquals("red", updatedProject.getTextColor());
        assertEquals("OLD", updatedProject.getTicketPrefix()); // Should be unchanged because it wasn't in patch
        assertEquals(Permission.FORBID, updatedProject.getExternalUserPermission()); // Should be unchanged
        
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testExecute_UpdateExternalPermission() {
        UUID ownerId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, ownerId, "Owner", "Owner", Permission.OWNER)))
                .externalUserPermission(Permission.FORBID)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Map<String, String> patch = Map.of(
                "externalUserPermission", "VIEWER"
        );

        Project updatedProject = useCase.execute(1L, ownerId, patch);

        assertNotNull(updatedProject);
        assertEquals(Permission.VIEWER, updatedProject.getExternalUserPermission());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testExecute_ThrowsExceptionIfProjectNotFound() {
        UUID requesterId = UUID.randomUUID();
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(1L, requesterId, Map.of());
        });
    }

    @Test
    void testExecute_ThrowsExceptionIfUserDoesNotHavePermission() {
        UUID editorId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR))) // Editor is not enough for project settings update
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));

        assertThrows(SecurityException.class, () -> {
            useCase.execute(1L, editorId, Map.of("name", "New Name"));
        });
    }

    @Test
    void testExecute_ThrowsIfNameBlank() {
        UUID adminId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, adminId, "Admin", "Admin", Permission.ADMIN)))
                .name("Existing")
                .ticketPrefix("EX")
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, adminId, Map.of("name", "")));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTicketPrefixTooLong() {
        UUID adminId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, adminId, "Admin", "Admin", Permission.ADMIN)))
                .name("Existing")
                .ticketPrefix("EX")
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, adminId, Map.of("ticketPrefix", "TOOLONGPREFIX")));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfNameContainsSectionSign() {
        UUID adminId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, adminId, "Admin", "Admin", Permission.ADMIN)))
                .name("Existing")
                .ticketPrefix("EX")
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, adminId, Map.of("name", "§bad name")));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_TicketPrefixNormalizedToUppercase() {
        UUID adminId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, adminId, "Admin", "Admin", Permission.ADMIN)))
                .name("Existing")
                .ticketPrefix("EX")
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Project result = useCase.execute(1L, adminId, Map.of("ticketPrefix", "abc"));

        assertEquals("ABC", result.getTicketPrefix());
    }
}
