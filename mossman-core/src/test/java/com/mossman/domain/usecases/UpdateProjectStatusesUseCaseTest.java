package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Status;
import com.mossman.domain.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateProjectStatusesUseCaseTest {

    private ProjectRepository projectRepository;
    private UpdateProjectStatusesUseCase useCase;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        useCase = new UpdateProjectStatusesUseCase(projectRepository);
    }

    @Test
    void testExecute_Success() {
        UUID editorId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .statuses(List.of(new Status("OPEN", "Open", "blue")))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        List<Status> newStatuses = List.of(
                new Status("OPEN", "Open", "blue"),
                new Status("IN_PROGRESS", "In Progress", "yellow")
        );

        Project updatedProject = useCase.execute(1L, editorId, newStatuses);

        assertNotNull(updatedProject);
        assertEquals(2, updatedProject.getStatuses().size());
        assertEquals("IN_PROGRESS", updatedProject.getStatuses().get(1).key());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testExecute_ThrowsExceptionIfProjectNotFound() {
        UUID requesterId = UUID.randomUUID();
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(1L, requesterId, List.of());
        });
    }

    @Test
    void testExecute_ThrowsExceptionIfUserDoesNotHavePermission() {
        UUID viewerId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, viewerId, "Viewer", "Viewer", Permission.VIEWER)))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));

        assertThrows(SecurityException.class, () -> {
            useCase.execute(1L, viewerId, List.of());
        });
    }

    @Test
    void testExecute_ThrowsExceptionIfStatusKeysNotUnique() {
        UUID editorId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));

        List<Status> duplicateStatuses = List.of(
                new Status("OPEN", "Open", "blue"),
                new Status("open", "open", "red") // Duplicate key ignoring case
        );

        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(1L, editorId, duplicateStatuses);
        });

        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfStatusKeyBlank() {
        UUID editorId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, editorId, List.of(new Status("", "", "blue"))));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfStatusKeyContainsInvalidChars() {
        UUID editorId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, editorId, List.of(new Status("§OPEN", "§OPEN", ""))));
        verify(projectRepository, never()).save(any());
    }
}
