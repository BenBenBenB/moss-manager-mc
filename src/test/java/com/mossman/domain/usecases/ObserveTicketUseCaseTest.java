package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Ticket;
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

class ObserveTicketUseCaseTest {

    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private ObserveTicketUseCase useCase;

    private UUID requesterId;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        useCase = new ObserveTicketUseCase(ticketRepository, projectRepository);

        requesterId = UUID.randomUUID();

        project = Project.builder()
                .id(1L)
                .members(List.of(new Member(1, requesterId, "user", "", Permission.VIEWER)))
                .externalUserPermission(Permission.FORBID)
                .build();

        ticket = new Ticket(10L, 1L, 1, "Test", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                requesterId, Collections.emptyList(), 0L, 0L, null);
    }

    @Test
    void testExecute_AddsObserver() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(10L, requesterId);

        assertTrue(result.getObservers().contains(requesterId));
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void testExecute_ThrowsIfAlreadyObserving() {
        Ticket alreadyWatching = new Ticket(10L, 1L, 1, "Test", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), List.of(requesterId),
                requesterId, Collections.emptyList(), 0L, 0L, null);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(alreadyWatching));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, requesterId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfForbidPermission() {
        UUID outsider = UUID.randomUUID();
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () -> useCase.execute(10L, outsider));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTicketNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, requesterId));
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, requesterId));
    }
}
