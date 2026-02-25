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

class TransferOwnershipUseCaseTest {

    private ProjectRepository projectRepository;
    private TransferOwnershipUseCase useCase;

    private UUID ownerId;
    private UUID editorId;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        useCase = new TransferOwnershipUseCase(projectRepository);
        ownerId = UUID.randomUUID();
        editorId = UUID.randomUUID();
    }

    private Project buildProject() {
        return Project.builder()
                .id(1L)
                .members(List.of(
                        new Member(1, ownerId, "owner", "Owner", Permission.OWNER),
                        new Member(2, editorId, "editor", "Editor", Permission.EDITOR)
                ))
                .externalUserPermission(Permission.FORBID)
                .build();
    }

    @Test
    void testExecute_OwnerTransferSucceeds() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        Project result = useCase.execute(1L, ownerId, editorId);

        // New owner should now be OWNER
        Member newOwner = result.getMembers().stream()
                .filter(m -> m.uuid().equals(editorId))
                .findFirst().orElseThrow();
        assertEquals(Permission.OWNER, newOwner.permission());

        // Old owner should be demoted to ADMIN
        Member oldOwner = result.getMembers().stream()
                .filter(m -> m.uuid().equals(ownerId))
                .findFirst().orElseThrow();
        assertEquals(Permission.ADMIN, oldOwner.permission());

        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testExecute_NonOwnerCannotTransfer() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(1L, editorId, ownerId));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_TargetMustBeExistingMember() {
        Project project = buildProject();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        UUID nonMemberId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, nonMemberId));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(1L, ownerId, editorId));
    }
}
