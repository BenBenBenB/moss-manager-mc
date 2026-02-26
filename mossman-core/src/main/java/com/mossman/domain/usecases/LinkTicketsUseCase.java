package com.mossman.domain.usecases;

import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.RelationshipType;
import com.mossman.domain.entities.TicketRelationship;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRelationshipRepository;
import com.mossman.domain.repositories.TicketRepository;

import java.util.UUID;

public class LinkTicketsUseCase {
    private final TicketRelationshipRepository relRepository;
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;

    public LinkTicketsUseCase(TicketRelationshipRepository relRepository,
                              TicketRepository ticketRepository,
                              ProjectRepository projectRepository) {
        this.relRepository = relRepository;
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
    }

    public TicketRelationship execute(long sourceTicketId, long targetTicketId, String type, UUID requesterId) {
        if (sourceTicketId == targetTicketId) {
            throw new IllegalArgumentException("A ticket cannot be linked to itself");
        }

        var sourceTicket = ticketRepository.findById(sourceTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Source ticket not found: id=" + sourceTicketId));

        var project = projectRepository.findById(sourceTicket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + sourceTicket.getProjectId()));

        PermissionChecker.requireProjectEditor(project, requesterId);

        RelationshipType matchedType = project.getRelationshipTypes().stream()
                .filter(rt -> rt.key().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Relationship type not found in project: " + type));

        var targetTicket = ticketRepository.findById(targetTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Target ticket not found: id=" + targetTicketId));

        if (targetTicket.getProjectId() != sourceTicket.getProjectId()) {
            throw new IllegalArgumentException("Cannot link tickets from different projects");
        }

        relRepository.findBySourceAndTargetAndType(sourceTicketId, targetTicketId, matchedType.key())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Relationship already exists");
                });

        return relRepository.save(new TicketRelationship(0, matchedType.key(), sourceTicketId, targetTicketId));
    }
}
