package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.RelationshipType;
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

class UpdateProjectRelationshipTypesUseCaseTest {

    private ProjectRepository projectRepository;
    private UpdateProjectRelationshipTypesUseCase useCase;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        useCase = new UpdateProjectRelationshipTypesUseCase(projectRepository);
    }

    @Test
    void testExecute_Success() {
        UUID editorId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .relationshipTypes(List.of(new RelationshipType("BLOCKS", "Blocks", "Blocks", "Blocked by", "red")))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        List<RelationshipType> newTypes = List.of(
                new RelationshipType("BLOCKS", "Blocks", "Blocks", "Blocked by", "red"),
                new RelationshipType("RELATES", "Relates To", "Relates to", "Relates to", "blue")
        );

        Project updatedProject = useCase.execute(1L, editorId, newTypes);

        assertNotNull(updatedProject);
        assertEquals(2, updatedProject.getRelationshipTypes().size());
        assertEquals("RELATES", updatedProject.getRelationshipTypes().get(1).key());
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
    void testExecute_ThrowsExceptionIfRelationshipTypeKeysNotUnique() {
        UUID editorId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));

        List<RelationshipType> duplicateTypes = List.of(
                new RelationshipType("RELATES", "Relates To", "Relates to", "Relates to", "blue"),
                new RelationshipType("relates", "relates", "Duplicates", "Duplicated by", "red") // Duplicate key ignoring case
        );

        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(1L, editorId, duplicateTypes);
        });

        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfRelTypeKeyBlank() {
        UUID editorId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, editorId, List.of(new RelationshipType("", "", "blocks", "is blocked by", ""))));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfRelTypeDescriptionContainsSectionSign() {
        UUID editorId = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, editorId, List.of(new RelationshipType("BLOCKS", "BLOCKS", "§blocks", "is blocked by", ""))));
        verify(projectRepository, never()).save(any());
    }
}
