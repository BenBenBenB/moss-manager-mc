package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RemoveMemberUseCaseTest {

    private ProjectRepository projectRepository;
    private RemoveMemberUseCase useCase;

    private UUID ownerId;
    private UUID adminId;
    private UUID editorId;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        useCase = new RemoveMemberUseCase(projectRepository);
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
    void testExecute_OwnerCanRemoveEditor() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Project result = useCase.execute(1L, ownerId, editorId);

        assertEquals(2, result.getMembers().size());
        assertTrue(result.getMembers().stream().noneMatch(m -> m.uuid().equals(editorId)));
    }

    @Test
    void testExecute_AdminCanRemoveEditor() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Project result = useCase.execute(1L, adminId, editorId);

        assertEquals(2, result.getMembers().size());
    }

    @Test
    void testExecute_AdminCannotRemoveOwner() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, adminId, ownerId));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_AdminCannotRemoveOtherAdmin() {
        UUID admin2Id = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(
                        new Member(1, ownerId, "owner", "Owner", Permission.OWNER),
                        new Member(2, adminId, "admin1", "Admin", Permission.ADMIN),
                        new Member(3, admin2Id, "admin2", "Admin2", Permission.ADMIN)
                ))
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, adminId, admin2Id));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_OwnerCannotBeRemoved() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, ownerId, ownerId));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfMemberNotFound() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, UUID.randomUUID()));
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, editorId));
    }
}
