package com.mossman.domain.usecases;

import com.mossman.domain.entities.*;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRelationshipRepository;
import com.mossman.domain.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LinkTicketsUseCaseTest {

    private TicketRelationshipRepository relRepository;
    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private LinkTicketsUseCase useCase;

    private UUID editorId;
    private UUID viewerId;
    private Project project;
    private Ticket sourceTicket;
    private Ticket targetTicket;

    @BeforeEach
    void setUp() {
        relRepository = mock(TicketRelationshipRepository.class);
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        useCase = new LinkTicketsUseCase(relRepository, ticketRepository, projectRepository);

        editorId = UUID.randomUUID();
        viewerId = UUID.randomUUID();

        project = Project.builder()
                .id(1L)
                .ticketPrefix("MOSS")
                .members(List.of(
                        new Member(1, editorId, "editor", "", Permission.EDITOR),
                        new Member(2, viewerId, "viewer", "", Permission.VIEWER)
                ))
                .relationshipTypes(List.of(
                        new RelationshipType("BLOCKS", "Blocks", "blocks", "is blocked by", null)
                ))
                .externalUserPermission(Permission.FORBID)
                .build();

        sourceTicket = new Ticket(10L, 1L, 1, "Source", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                editorId, Collections.emptyList(), 0L, 0L, null);

        targetTicket = new Ticket(20L, 1L, 2, "Target", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                editorId, Collections.emptyList(), 0L, 0L, null);
    }

    @Test
    void testExecute_Success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(targetTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(relRepository.findBySourceAndTargetAndType(10L, 20L, "BLOCKS")).thenReturn(Optional.empty());
        TicketRelationship saved = new TicketRelationship(1L, "BLOCKS", 10L, 20L);
        when(relRepository.save(any())).thenReturn(saved);

        TicketRelationship result = useCase.execute(10L, 20L, "BLOCKS", editorId);

        assertEquals("BLOCKS", result.type());
        assertEquals(10L, result.sourceTicketId());
        assertEquals(20L, result.targetTicketId());
        verify(relRepository).save(any());
    }

    @Test
    void testExecute_ThrowsIfSameTicket() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 10L, "BLOCKS", editorId));
        verifyNoInteractions(relRepository);
    }

    @Test
    void testExecute_ThrowsIfSourceNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 20L, "BLOCKS", editorId));
    }

    @Test
    void testExecute_ThrowsIfTypeNotInProject() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 20L, "UNKNOWN_TYPE", editorId));
        verify(relRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTargetNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(ticketRepository.findById(20L)).thenReturn(Optional.empty());
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 20L, "BLOCKS", editorId));
        verify(relRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfDifferentProject() {
        Ticket otherProjectTicket = new Ticket(20L, 99L, 2, "Other", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                editorId, Collections.emptyList(), 0L, 0L, null);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(otherProjectTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 20L, "BLOCKS", editorId));
        verify(relRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfDuplicate() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(targetTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(relRepository.findBySourceAndTargetAndType(10L, 20L, "BLOCKS"))
                .thenReturn(Optional.of(new TicketRelationship(1L, "BLOCKS", 10L, 20L)));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 20L, "BLOCKS", editorId));
        verify(relRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfInsufficientPermission() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () -> useCase.execute(10L, 20L, "BLOCKS", viewerId));
        verify(relRepository, never()).save(any());
    }

    @Test
    void testExecute_TypeMatchIsCaseInsensitive() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(targetTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(relRepository.findBySourceAndTargetAndType(10L, 20L, "BLOCKS")).thenReturn(Optional.empty());
        TicketRelationship saved = new TicketRelationship(1L, "BLOCKS", 10L, 20L);
        when(relRepository.save(any())).thenReturn(saved);

        // Use lowercase "blocks" — should match "BLOCKS" in project
        TicketRelationship result = useCase.execute(10L, 20L, "blocks", editorId);

        // Canonical name from project should be used
        verify(relRepository).save(argThat(r -> "BLOCKS".equals(r.type())));
    }
}
