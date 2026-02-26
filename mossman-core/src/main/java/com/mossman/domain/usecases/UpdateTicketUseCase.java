package com.mossman.domain.usecases;

import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketUpdatedEvent;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.validation.EntityValidator;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateTicketUseCase {
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public UpdateTicketUseCase(TicketRepository ticketRepository, ProjectRepository projectRepository, DomainEventBus eventBus) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    /**
     * @param ticketId    DB id of the ticket to patch
     * @param requesterId UUID of the user requesting the update
     * @param patch       map of field name → new string value (from SNBT)
     * @return the updated Ticket
     * @throws IllegalArgumentException if the ticket or project does not exist
     * @throws SecurityException if the requester does not have EDITOR permission
     */
    public Ticket execute(long ticketId, java.util.UUID requesterId, Map<String, String> patch) {
        Ticket current = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + ticketId));

        com.mossman.domain.entities.Project project = projectRepository.findById(current.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + current.getProjectId()));

        com.mossman.domain.auth.PermissionChecker.requireProjectEditor(project, requesterId);

        if (patch.containsKey("title"))       EntityValidator.requireValidTicketTitle(patch.get("title"));
        if (patch.containsKey("description")) EntityValidator.requireValidTicketDescription(patch.get("description"));
        if (patch.containsKey("labels")) {
            parseLabelsFromPatch(patch.get("labels")).forEach(EntityValidator::requireValidLabel);
        }

        String title       = patch.getOrDefault("title",       current.getTitle());
        String description = patch.getOrDefault("description", current.getDescription() != null ? current.getDescription() : "");
        String type        = patch.getOrDefault("type",        current.getType());
        String status      = patch.getOrDefault("status",      current.getStatus()).toUpperCase();
        Priority priority  = patch.containsKey("priority")
                ? Priority.valueOf(patch.get("priority").toUpperCase())
                : current.getPriority();
        List<String> labels = patch.containsKey("labels")
                ? parseLabelsFromPatch(patch.get("labels"))
                : current.getLabels();

        Ticket updated = new Ticket(
                current.getId(),
                current.getProjectId(),
                current.getTicketNumber(),
                title,
                description,
                type,
                status,
                priority,
                current.getAssignees(),
                current.getObservers(),
                current.getCreator(),
                labels,
                current.getCreatedAt(),
                System.currentTimeMillis(),
                current.getSprintId()
        );

        Ticket saved = ticketRepository.save(updated);
        eventBus.publish(new TicketUpdatedEvent(saved, requesterId, Instant.now()));
        return saved;
    }

    /**
     * Parses a {@code |}-delimited labels string (as encoded by the command layer) into a list.
     * An empty or blank value yields an empty list (clearing all labels).
     */
    private static List<String> parseLabelsFromPatch(String encoded) {
        if (encoded == null || encoded.isBlank()) return Collections.emptyList();
        return Arrays.stream(encoded.split("\\|"))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
