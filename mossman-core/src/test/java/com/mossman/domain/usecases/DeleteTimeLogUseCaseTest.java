package com.mossman.domain.usecases;

import com.mossman.domain.entities.*;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.domain.repositories.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteTimeLogUseCaseTest {

    private TimeLogRepository timeLogRepository;
    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private DeleteTimeLogUseCase useCase;

    private UUID editorId;
    private UUID authorId;
    private UUID outsiderId;
    private Project project;
    private Ticket ticket;
    private TimeLog timeLog;

    @BeforeEach
    void setUp() {
        timeLogRepository = mock(TimeLogRepository.class);
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        useCase = new DeleteTimeLogUseCase(timeLogRepository, ticketRepository, projectRepository);

        editorId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        outsiderId = UUID.randomUUID();

        project = Project.builder()
                .id(1L)
                .members(List.of(
                        new Member(1, editorId, "editor", "", Permission.EDITOR),
                        new Member(2, authorId, "author", "", Permission.CREATOR)
                ))
                .externalUserPermission(Permission.FORBID)
                .build();

        ticket = new Ticket(10L, 1L, 1, "Test", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                editorId, Collections.emptyList(), 0L, 0L, null);

        timeLog = new TimeLog(5L, 10L, authorId, "author", 60, "did work", System.currentTimeMillis());
    }

    @Test
    void testExecute_SuccessAsAuthor() {
        when(timeLogRepository.findById(5L)).thenReturn(Optional.of(timeLog));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertDoesNotThrow(() -> useCase.execute(5L, authorId));
        verify(timeLogRepository).delete(5L);
    }

    @Test
    void testExecute_SuccessAsEditor() {
        when(timeLogRepository.findById(5L)).thenReturn(Optional.of(timeLog));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertDoesNotThrow(() -> useCase.execute(5L, editorId));
        verify(timeLogRepository).delete(5L);
    }

    @Test
    void testExecute_ThrowsIfLogNotFound() {
        when(timeLogRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(5L, authorId));
        verify(timeLogRepository, never()).delete(anyLong());
    }

    @Test
    void testExecute_ThrowsIfInsufficientPermission() {
        when(timeLogRepository.findById(5L)).thenReturn(Optional.of(timeLog));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () -> useCase.execute(5L, outsiderId));
        verify(timeLogRepository, never()).delete(anyLong());
    }
}
