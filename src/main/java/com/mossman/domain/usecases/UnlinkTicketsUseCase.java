package com.mossman.domain.usecases;

import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRelationshipRepository;
import com.mossman.domain.repositories.TicketRepository;

import java.util.UUID;

public class UnlinkTicketsUseCase {
    private final TicketRelationshipRepository relRepository;
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;

    public UnlinkTicketsUseCase(TicketRelationshipRepository relRepository,
                                TicketRepository ticketRepository,
                                ProjectRepository projectRepository) {
        this.relRepository = relRepository;
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
    }

    public void execute(long sourceTicketId, long targetTicketId, String type, UUID requesterId) {
        var sourceTicket = ticketRepository.findById(sourceTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Source ticket not found: id=" + sourceTicketId));

        var project = projectRepository.findById(sourceTicket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + sourceTicket.getProjectId()));

        PermissionChecker.requireProjectEditor(project, requesterId);

        var rel = relRepository.findBySourceAndTargetAndType(sourceTicketId, targetTicketId, type)
                .orElseThrow(() -> new IllegalArgumentException("Relationship not found"));

        relRepository.delete(rel.id());
    }
}
