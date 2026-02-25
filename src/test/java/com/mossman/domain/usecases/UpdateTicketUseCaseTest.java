package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketUpdatedEvent;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateTicketUseCaseTest {

    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private DomainEventBus eventBus;
    private UpdateTicketUseCase useCase;

    private UUID editorId;
    private UUID viewerId;
    private Project project;
    private Ticket existingTicket;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(DomainEventBus.class);
        useCase = new UpdateTicketUseCase(ticketRepository, projectRepository, eventBus);

        editorId = UUID.randomUUID();
        viewerId = UUID.randomUUID();

        project = Project.builder()
                .id(1L)
                .members(List.of(
                        new Member(1, editorId, "editor", "Editor", Permission.EDITOR),
                        new Member(2, viewerId, "viewer", "Viewer", Permission.VIEWER)
                ))
                .externalUserPermission(Permission.FORBID)
                .build();

        existingTicket = new Ticket(
                42L, 1L, 1, "Old Title", "Old Desc",
                "BUG", "OPEN", Priority.MEDIUM,
                Collections.emptyList(), Collections.emptyList(),
                editorId, Collections.emptyList(),
                System.currentTimeMillis(), System.currentTimeMillis(), null
        );
    }

    @Test
    void testExecute_EditorCanUpdateTitle() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(42L, editorId, Map.of("title", "New Title"));

        assertEquals("New Title", result.getTitle());
        assertEquals("Old Desc", result.getDescription()); // not changed
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void testExecute_EditorCanUpdateStatus() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(42L, editorId, Map.of("status", "in_progress"));

        assertEquals("IN_PROGRESS", result.getStatus());
    }

    @Test
    void testExecute_EditorCanUpdatePriority() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(42L, editorId, Map.of("priority", "HIGH"));

        assertEquals(Priority.HIGH, result.getPriority());
    }

    @Test
    void testExecute_ViewerCannotUpdate() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(SecurityException.class, () ->
                useCase.execute(42L, viewerId, Map.of("title", "Hacked")));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_UnpatchedFieldsArePreserved() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(42L, editorId, Map.of("description", "Updated Desc"));

        assertEquals("Old Title", result.getTitle());      // unchanged
        assertEquals("Updated Desc", result.getDescription());
        assertEquals(Priority.MEDIUM, result.getPriority()); // unchanged
    }

    @Test
    void testExecute_ThrowsIfTicketNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(99L, editorId, Map.of("title", "X")));
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(42L, editorId, Map.of("title", "X")));
    }

    @Test
    void testExecute_ThrowsIfTitleBlank() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(42L, editorId, Map.of("title", "")));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTitleTooLong() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(42L, editorId, Map.of("title", "A".repeat(129))));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTitleContainsSectionSign() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(42L, editorId, Map.of("title", "§bad title")));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_PublishesTicketUpdatedEvent() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.of(existingTicket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        useCase.execute(42L, editorId, Map.of("title", "New Title"));

        verify(eventBus).publish(argThat(e -> e instanceof TicketUpdatedEvent evt
                && evt.requesterId().equals(editorId)));
    }
}
