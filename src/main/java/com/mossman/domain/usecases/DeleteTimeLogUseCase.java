package com.mossman.domain.usecases;

import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.domain.repositories.TimeLogRepository;

import java.util.UUID;

public class DeleteTimeLogUseCase {
    private final TimeLogRepository timeLogRepository;
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;

    public DeleteTimeLogUseCase(TimeLogRepository timeLogRepository,
                                TicketRepository ticketRepository,
                                ProjectRepository projectRepository) {
        this.timeLogRepository = timeLogRepository;
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
    }

    public void execute(long logId, UUID requesterId) {
        var log = timeLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Time log not found: id=" + logId));

        var ticket = ticketRepository.findById(log.ticketId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + log.ticketId()));

        var project = projectRepository.findById(ticket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + ticket.getProjectId()));

        Permission perm = PermissionChecker.getEffectivePermission(project, requesterId);
        boolean isEditor = perm == Permission.OWNER || perm == Permission.ADMIN || perm == Permission.EDITOR;
        boolean isAuthor = log.workerId().equals(requesterId);

        if (!isEditor && !isAuthor) {
            throw new SecurityException("Insufficient permission to delete this time log.");
        }

        timeLogRepository.delete(logId);
    }
}
