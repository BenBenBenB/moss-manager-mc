package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketCreatedEvent;
import com.mossman.domain.repositories.ProjectRepository;
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

class CreateTicketUseCaseTest {

    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private DomainEventBus eventBus;
    private CreateTicketUseCase useCase;

    private UUID creatorId;
    private Project project;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(DomainEventBus.class);
        useCase = new CreateTicketUseCase(ticketRepository, projectRepository, eventBus);

        creatorId = UUID.randomUUID();
        project = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, creatorId, "creator", "Dev", Permission.CREATOR)))
                .externalUserPermission(Permission.FORBID)
                .build();
    }

    private Ticket buildTicket() {
        return new Ticket(0, 1L, 1, "Fix bug", "desc",
                "BUG", "OPEN", Priority.MEDIUM,
                Collections.emptyList(), Collections.emptyList(),
                creatorId, Collections.emptyList(),
                System.currentTimeMillis(), System.currentTimeMillis(), null);
    }

    @Test
    void testExecute_Success() {
        Ticket ticket = buildTicket();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        Ticket result = useCase.execute(ticket, creatorId);

        assertNotNull(result);
        assertEquals("Fix bug", result.getTitle());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(eventBus, times(1)).publish(any(TicketCreatedEvent.class));
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(projectRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(buildTicket(), creatorId));
    }

    @Test
    void testExecute_ThrowsIfViewerPermission() {
        UUID viewerId = UUID.randomUUID();
        Project projectWithViewer = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, viewerId, "viewer", "Viewer", Permission.VIEWER)))
                .externalUserPermission(Permission.FORBID)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectWithViewer));

        assertThrows(SecurityException.class, () ->
                useCase.execute(buildTicket(), viewerId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfForbiddenPermission() {
        UUID outsiderId = UUID.randomUUID(); // not a member, external is FORBID -> FORBID
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(buildTicket(), outsiderId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTitleBlank() {
        Ticket ticket = new Ticket(0, 1L, 1, "", "desc",
                "BUG", "OPEN", Priority.MEDIUM,
                Collections.emptyList(), Collections.emptyList(),
                creatorId, Collections.emptyList(),
                System.currentTimeMillis(), System.currentTimeMillis(), null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(ticket, creatorId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTitleTooLong() {
        Ticket ticket = new Ticket(0, 1L, 1, "A".repeat(129), "desc",
                "BUG", "OPEN", Priority.MEDIUM,
                Collections.emptyList(), Collections.emptyList(),
                creatorId, Collections.emptyList(),
                System.currentTimeMillis(), System.currentTimeMillis(), null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(ticket, creatorId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTitleContainsSectionSign() {
        Ticket ticket = new Ticket(0, 1L, 1, "§red title", "desc",
                "BUG", "OPEN", Priority.MEDIUM,
                Collections.emptyList(), Collections.emptyList(),
                creatorId, Collections.emptyList(),
                System.currentTimeMillis(), System.currentTimeMillis(), null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(ticket, creatorId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_AddsCreatorToObservers() {
        Ticket ticket = buildTicket();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(ticket, creatorId);

        assertTrue(result.getObservers().contains(creatorId));
    }
}
