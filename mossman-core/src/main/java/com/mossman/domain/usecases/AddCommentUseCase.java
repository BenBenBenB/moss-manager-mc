package com.mossman.domain.usecases;

import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Comment;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketCommentedEvent;
import com.mossman.domain.repositories.CommentRepository;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;

import java.time.Instant;
import java.util.UUID;

public class AddCommentUseCase {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public AddCommentUseCase(CommentRepository commentRepository,
                             TicketRepository ticketRepository,
                             ProjectRepository projectRepository,
                             DomainEventBus eventBus) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    public Comment execute(long ticketId, UUID authorId, String authorName, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Comment message must not be blank.");
        }
        if (message.length() > 1024) {
            throw new IllegalArgumentException("Comment message must not exceed 1024 characters.");
        }

        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + ticketId));

        var project = projectRepository.findById(ticket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + ticket.getProjectId()));

        Permission perm = PermissionChecker.getEffectivePermission(project, authorId);
        if (!isAtLeastCreator(perm)) {
            throw new SecurityException("Insufficient permission to comment on this ticket.");
        }

        Comment saved = commentRepository.save(
                new Comment(0, ticketId, authorId, authorName, message, System.currentTimeMillis()));

        eventBus.publish(new TicketCommentedEvent(saved, ticket, authorId, Instant.now()));

        return saved;
    }

    private static boolean isAtLeastCreator(Permission perm) {
        return perm == Permission.OWNER || perm == Permission.ADMIN
                || perm == Permission.EDITOR || perm == Permission.CREATOR;
    }
}
