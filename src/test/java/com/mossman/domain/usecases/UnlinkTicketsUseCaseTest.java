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
import static org.mockito.Mockito.*;

class UnlinkTicketsUseCaseTest {

    private TicketRelationshipRepository relRepository;
    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private UnlinkTicketsUseCase useCase;

    private UUID editorId;
    private UUID viewerId;
    private Project project;
    private Ticket sourceTicket;

    @BeforeEach
    void setUp() {
        relRepository = mock(TicketRelationshipRepository.class);
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        useCase = new UnlinkTicketsUseCase(relRepository, ticketRepository, projectRepository);

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
    }

    @Test
    void testExecute_Success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        TicketRelationship rel = new TicketRelationship(5L, "BLOCKS", 10L, 20L);
        when(relRepository.findBySourceAndTargetAndType(10L, 20L, "BLOCKS")).thenReturn(Optional.of(rel));

        useCase.execute(10L, 20L, "BLOCKS", editorId);

        verify(relRepository).delete(5L);
    }

    @Test
    void testExecute_ThrowsIfNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(relRepository.findBySourceAndTargetAndType(10L, 20L, "BLOCKS")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 20L, "BLOCKS", editorId));
        verify(relRepository, never()).delete(anyLong());
    }

    @Test
    void testExecute_ThrowsIfInsufficientPermission() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sourceTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () -> useCase.execute(10L, 20L, "BLOCKS", viewerId));
        verify(relRepository, never()).delete(anyLong());
    }

    @Test
    void testExecute_ThrowsIfSourceTicketNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, 20L, "BLOCKS", editorId));
        verify(relRepository, never()).delete(anyLong());
    }
}
