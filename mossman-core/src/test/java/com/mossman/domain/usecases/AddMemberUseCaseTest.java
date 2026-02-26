package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddMemberUseCaseTest {

    private ProjectRepository projectRepository;
    private AddMemberUseCase useCase;

    private UUID ownerId;
    private UUID adminId;
    private UUID editorId;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        useCase = new AddMemberUseCase(projectRepository);

        ownerId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        editorId = UUID.randomUUID();
    }

    private Project buildProject() {
        return Project.builder()
                .id(1L)
                .members(new ArrayList<>(List.of(
                        new Member(1, ownerId, "owner", "Owner", Permission.OWNER),
                        new Member(2, adminId, "admin", "Admin", Permission.ADMIN)
                )))
                .externalUserPermission(Permission.FORBID)
                .build();
    }

    @Test
    void testExecute_OwnerCanAddAdminMember() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Member newMember = new Member(0, editorId, "newguy", "Dev", Permission.ADMIN);
        Project result = useCase.execute(1L, ownerId, newMember);

        assertEquals(3, result.getMembers().size());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testExecute_AdminCanAddEditorMember() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Member newMember = new Member(0, editorId, "neweditor", "Editor", Permission.EDITOR);
        Project result = useCase.execute(1L, adminId, newMember);

        assertEquals(3, result.getMembers().size());
    }

    @Test
    void testExecute_AdminCannotAddAdminMember() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Member newMember = new Member(0, UUID.randomUUID(), "newadmin", "Admin", Permission.ADMIN);
        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, adminId, newMember));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_AdminCannotAddOwnerMember() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Member newMember = new Member(0, UUID.randomUUID(), "newowner", "Owner", Permission.OWNER);
        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, adminId, newMember));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfEditorTriesToAddMember() {
        UUID editorRequester = UUID.randomUUID();
        Project project = Project.builder()
                .id(1L)
                .members(List.of(new Member(1, editorRequester, "editor", "Editor", Permission.EDITOR)))
                .externalUserPermission(Permission.FORBID)
                .build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, editorRequester, new Member(0, UUID.randomUUID(), "x", "x", Permission.VIEWER)));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, new Member(0, UUID.randomUUID(), "x", "x", Permission.VIEWER)));
    }

    @Test
    void testExecute_ThrowsIfAlreadyMember() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // adminId is already a member of the project built by buildProject()
        Member duplicate = new Member(0, adminId, "admin", "", Permission.EDITOR);
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, duplicate));
        verify(projectRepository, never()).save(any());
    }
}
