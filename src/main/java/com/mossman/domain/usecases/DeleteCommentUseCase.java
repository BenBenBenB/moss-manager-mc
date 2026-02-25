package com.mossman.domain.usecases;

import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.repositories.CommentRepository;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;

import java.util.UUID;

public class DeleteCommentUseCase {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;

    public DeleteCommentUseCase(CommentRepository commentRepository,
                                TicketRepository ticketRepository,
                                ProjectRepository projectRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
    }

    public void execute(long commentId, UUID requesterId) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: id=" + commentId));

        var ticket = ticketRepository.findById(comment.ticketId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + comment.ticketId()));

        var project = projectRepository.findById(ticket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + ticket.getProjectId()));

        Permission perm = PermissionChecker.getEffectivePermission(project, requesterId);
        boolean isEditor = perm == Permission.OWNER || perm == Permission.ADMIN || perm == Permission.EDITOR;
        boolean isAuthor = comment.authorId().equals(requesterId);

        if (!isEditor && !isAuthor) {
            throw new SecurityException("Insufficient permission to delete this comment.");
        }

        commentRepository.delete(commentId);
    }
}
