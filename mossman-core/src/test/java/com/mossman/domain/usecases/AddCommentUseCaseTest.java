package com.mossman.domain.usecases;

import com.mossman.domain.entities.*;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketCommentedEvent;
import com.mossman.domain.repositories.CommentRepository;
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

class AddCommentUseCaseTest {

    private CommentRepository commentRepository;
    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private DomainEventBus eventBus;
    private AddCommentUseCase useCase;

    private UUID editorId;
    private UUID forbiddenId;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(DomainEventBus.class);
        useCase = new AddCommentUseCase(commentRepository, ticketRepository, projectRepository, eventBus);

        editorId = UUID.randomUUID();
        forbiddenId = UUID.randomUUID();

        project = Project.builder()
                .id(1L)
                .members(List.of(new Member(1, editorId, "editor", "", Permission.EDITOR)))
                .externalUserPermission(Permission.FORBID)
                .build();

        ticket = new Ticket(10L, 1L, 1, "Test", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), Collections.emptyList(),
                editorId, Collections.emptyList(), 0L, 0L, null);
    }

    @Test
    void testExecute_Success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        Comment saved = new Comment(1L, 10L, editorId, "editor", "Hello world", System.currentTimeMillis());
        when(commentRepository.save(any())).thenReturn(saved);

        Comment result = useCase.execute(10L, editorId, "editor", "Hello world");

        assertNotNull(result);
        assertEquals("Hello world", result.message());
        verify(commentRepository).save(any());
        verify(eventBus).publish(any(TicketCommentedEvent.class));
    }

    @Test
    void testExecute_ThrowsIfMessageBlank() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, editorId, "editor", "  "));
        verifyNoInteractions(commentRepository, ticketRepository);
    }

    @Test
    void testExecute_ThrowsIfMessageTooLong() {
        String longMessage = "x".repeat(1025);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, editorId, "editor", longMessage));
        verifyNoInteractions(commentRepository, ticketRepository);
    }

    @Test
    void testExecute_ThrowsIfTicketNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, editorId, "editor", "msg"));
    }

    @Test
    void testExecute_ThrowsIfProjectNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, editorId, "editor", "msg"));
    }

    @Test
    void testExecute_ThrowsIfInsufficientPermission() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        assertThrows(SecurityException.class, () -> useCase.execute(10L, forbiddenId, "stranger", "msg"));
        verify(commentRepository, never()).save(any());
    }
}
