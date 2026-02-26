package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateMemberUseCaseTest {

    private ProjectRepository projectRepository;
    private UpdateMemberUseCase useCase;

    private UUID ownerId;
    private UUID adminId;
    private UUID editorId;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        useCase = new UpdateMemberUseCase(projectRepository);
        ownerId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        editorId = UUID.randomUUID();
    }

    private Project buildProject() {
        return Project.builder()
                .id(1L)
                .members(List.of(
                        new Member(1, ownerId, "owner", "Owner", Permission.OWNER),
                        new Member(2, adminId, "admin", "Admin", Permission.ADMIN),
                        new Member(3, editorId, "editor", "Editor", Permission.EDITOR)
                ))
                .externalUserPermission(Permission.FORBID)
                .build();
    }

    @Test
    void testExecute_OwnerCanUpdateTitle() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Project result = useCase.execute(1L, ownerId, editorId, Map.of("title", "Scrum Crafter"));

        Member updated = result.getMembers().stream()
                .filter(m -> m.uuid().equals(editorId))
                .findFirst().orElseThrow();
        assertEquals("Scrum Crafter", updated.title());
    }

    @Test
    void testExecute_OwnerCanPromoteEditorToAdmin() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Project result = useCase.execute(1L, ownerId, editorId, Map.of("permission", "ADMIN"));

        Member updated = result.getMembers().stream()
                .filter(m -> m.uuid().equals(editorId))
                .findFirst().orElseThrow();
        assertEquals(Permission.ADMIN, updated.permission());
    }

    @Test
    void testExecute_AdminCanUpdateEditorTitle() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Project result = useCase.execute(1L, adminId, editorId, Map.of("title", "Miner"));

        Member updated = result.getMembers().stream()
                .filter(m -> m.uuid().equals(editorId))
                .findFirst().orElseThrow();
        assertEquals("Miner", updated.title());
    }

    @Test
    void testExecute_AdminCannotPromoteToAdmin() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, adminId, editorId, Map.of("permission", "ADMIN")));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_AdminCannotModifyOwner() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, adminId, ownerId, Map.of("title", "New Title")));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_AdminCannotChangeOtherAdminPermission() {
        UUID admin2Id = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(
                        new Member(1, ownerId, "owner", "Owner", Permission.OWNER),
                        new Member(2, adminId, "admin1", "Admin1", Permission.ADMIN),
                        new Member(3, admin2Id, "admin2", "Admin2", Permission.ADMIN)
                ))
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, adminId, admin2Id, Map.of("permission", "EDITOR")));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfMemberNotFound() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, UUID.randomUUID(), Map.of("title", "X")));
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, editorId, Map.of()));
    }

    @Test
    void testExecute_ThrowsIfTitleTooLong() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, editorId, Map.of("title", "A".repeat(65))));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTitleContainsSectionSign() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, editorId, Map.of("title", "§Owner")));
        verify(projectRepository, never()).save(any());
    }
}
