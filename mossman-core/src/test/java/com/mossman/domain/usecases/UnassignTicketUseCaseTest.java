package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketUnassignedEvent;
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

class UnassignTicketUseCaseTest {

    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private DomainEventBus eventBus;
    private UnassignTicketUseCase useCase;

    private UUID editorId;
    private UUID viewerId;
    private UUID assigneeId;
    private Project project;
    private Ticket ticketWithAssignee;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(DomainEventBus.class);
        useCase = new UnassignTicketUseCase(ticketRepository, projectRepository, eventBus);

        editorId = UUID.randomUUID();
        viewerId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        project = Project.builder()
                .id(1L)
                .ticketPrefix("MOSS")
                .members(List.of(
                        new Member(1, editorId, "editor", "", Permission.EDITOR),
                        new Member(2, viewerId, "viewer", "", Permission.VIEWER)
                ))
                .externalUserPermission(Permission.FORBID)
                .build();

        ticketWithAssignee = new Ticket(10L, 1L, 1, "Test ticket", "", "Task", "OPEN",
                Priority.MEDIUM, List.of(assigneeId), Collections.emptyList(),
                editorId, Collections.emptyList(), 0L, 0L, null);
    }

    @Test
    void testExecute_UnassignsPlayerSuccessfully() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithAssignee));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(10L, editorId, assigneeId);

        assertFalse(result.getAssignees().contains(assigneeId));
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventBus).publish(any(TicketUnassignedEvent.class));
    }

    @Test
    void testExecute_ThrowsIfNotAssigned() {
        Ticket noAssignees = new Ticket(10L, 1L, 1, "Test", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                editorId, Collections.emptyList(), 0L, 0L, null);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(noAssignees));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, editorId, assigneeId));
        verify(ticketRepository, never()).save(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    void testExecute_ThrowsIfInsufficientPermission() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithAssignee));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () -> useCase.execute(10L, viewerId, assigneeId));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTicketNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, editorId, assigneeId));
    }

    @Test
    void testExecute_PublishesTicketUnassignedEvent() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithAssignee));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        useCase.execute(10L, editorId, assigneeId);

        verify(eventBus).publish(argThat(e -> e instanceof TicketUnassignedEvent evt
                && evt.assigneeId().equals(assigneeId)));
    }
}
