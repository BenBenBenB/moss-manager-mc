package com.mossman.domain.usecases;

import com.mossman.adapters.tui.DurationParser;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.TimeLog;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.domain.repositories.TimeLogRepository;

import java.util.UUID;

public class LogTimeUseCase {
    private final TimeLogRepository timeLogRepository;
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;

    public LogTimeUseCase(TimeLogRepository timeLogRepository,
                          TicketRepository ticketRepository,
                          ProjectRepository projectRepository) {
        this.timeLogRepository = timeLogRepository;
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
    }

    public TimeLog execute(long ticketId, UUID workerId, String workerName, String durationInput, String note) {
        long minutes = DurationParser.parse(durationInput);

        if (note != null && note.length() > 256) {
            throw new IllegalArgumentException("Note must not exceed 256 characters.");
        }
        String safeNote = note != null ? note : "";

        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + ticketId));

        var project = projectRepository.findById(ticket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + ticket.getProjectId()));

        Permission perm = PermissionChecker.getEffectivePermission(project, workerId);
        if (!isAtLeastCreator(perm)) {
            throw new SecurityException("Insufficient permission to log time on this ticket.");
        }

        return timeLogRepository.save(
                new TimeLog(0, ticketId, workerId, workerName, minutes, safeNote, System.currentTimeMillis()));
    }

    private static boolean isAtLeastCreator(Permission perm) {
        return perm == Permission.OWNER || perm == Permission.ADMIN
                || perm == Permission.EDITOR || perm == Permission.CREATOR;
    }
}
