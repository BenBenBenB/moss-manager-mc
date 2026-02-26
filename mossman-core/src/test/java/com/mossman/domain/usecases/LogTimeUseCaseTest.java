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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogTimeUseCaseTest {

    private TimeLogRepository timeLogRepository;
    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private LogTimeUseCase useCase;

    private UUID creatorId;
    private UUID viewerId;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        timeLogRepository = mock(TimeLogRepository.class);
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        useCase = new LogTimeUseCase(timeLogRepository, ticketRepository, projectRepository);

        creatorId = UUID.randomUUID();
        viewerId = UUID.randomUUID();

        project = Project.builder()
                .id(1L)
                .members(List.of(
                        new Member(1, creatorId, "creator", "", Permission.CREATOR),
                        new Member(2, viewerId, "viewer", "", Permission.VIEWER)
                ))
                .externalUserPermission(Permission.FORBID)
                .build();

        ticket = new Ticket(10L, 1L, 1, "Test ticket", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                creatorId, Collections.emptyList(), 0L, 0L, null);
    }

    @Test
    void testExecute_SuccessWithNote() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        TimeLog saved = new TimeLog(1L, 10L, creatorId, "creator", 150, "Fixed auth bug", System.currentTimeMillis());
        when(timeLogRepository.save(any())).thenReturn(saved);

        TimeLog result = useCase.execute(10L, creatorId, "creator", "2h 30m", "Fixed auth bug");

        assertNotNull(result);
        assertEquals(150, result.minutes());
        assertEquals("Fixed auth bug", result.note());
        verify(timeLogRepository).save(any());
    }

    @Test
    void testExecute_SuccessWithoutNote() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        TimeLog saved = new TimeLog(2L, 10L, creatorId, "creator", 60, "", System.currentTimeMillis());
        when(timeLogRepository.save(any())).thenReturn(saved);

        TimeLog result = useCase.execute(10L, creatorId, "creator", "1h", null);

        assertNotNull(result);
        assertEquals(60, result.minutes());
        verify(timeLogRepository).save(any());
    }

    @Test
    void testExecute_InvalidDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(10L, creatorId, "creator", "abc", null));
        verifyNoInteractions(timeLogRepository, ticketRepository);
    }

    @Test
    void testExecute_ZeroDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(10L, creatorId, "creator", "0m", null));
        verifyNoInteractions(timeLogRepository, ticketRepository);
    }

    @Test
    void testExecute_NoteTooLong() {
        String longNote = "x".repeat(257);
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(10L, creatorId, "creator", "1h", longNote));
        verifyNoInteractions(timeLogRepository, ticketRepository);
    }

    @Test
    void testExecute_TicketNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(10L, creatorId, "creator", "1h", null));
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void testExecute_ViewerRejected() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        assertThrows(SecurityException.class,
                () -> useCase.execute(10L, viewerId, "viewer", "1h", null));
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void testExecute_OutsiderRejected() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        UUID outsider = UUID.randomUUID();
        assertThrows(SecurityException.class,
                () -> useCase.execute(10L, outsider, "outsider", "1h", null));
        verify(timeLogRepository, never()).save(any());
    }
}
