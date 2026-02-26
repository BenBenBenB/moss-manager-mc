package com.mossman.domain.usecases;

import com.mossman.domain.entities.*;
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
import static org.mockito.Mockito.*;

class DeleteCommentUseCaseTest {

    private CommentRepository commentRepository;
    private TicketRepository ticketRepository;
    private ProjectRepository projectRepository;
    private DeleteCommentUseCase useCase;

    private UUID editorId;
    private UUID authorId;
    private UUID outsiderId;
    private Project project;
    private Ticket ticket;
    private Comment comment;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        ticketRepository = mock(TicketRepository.class);
        projectRepository = mock(ProjectRepository.class);
        useCase = new DeleteCommentUseCase(commentRepository, ticketRepository, projectRepository);

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

        comment = new Comment(5L, 10L, authorId, "author", "Hello", System.currentTimeMillis());
    }

    @Test
    void testExecute_SuccessAsAuthor() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertDoesNotThrow(() -> useCase.execute(5L, authorId));
        verify(commentRepository).delete(5L);
    }

    @Test
    void testExecute_SuccessAsEditor() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertDoesNotThrow(() -> useCase.execute(5L, editorId));
        verify(commentRepository).delete(5L);
    }

    @Test
    void testExecute_ThrowsIfCommentNotFound() {
        when(commentRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(5L, authorId));
        verify(commentRepository, never()).delete(anyLong());
    }

    @Test
    void testExecute_ThrowsIfInsufficientPermission() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // outsiderId is not the author and has no project permission
        assertThrows(SecurityException.class, () -> useCase.execute(5L, outsiderId));
        verify(commentRepository, never()).delete(anyLong());
    }
}
