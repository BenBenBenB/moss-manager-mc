package com.mossman.domain.usecases;

import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.validation.EntityValidator;
import com.mossman.domain.validation.TextColorValidator;

public class UpdateProjectTicketTypesUseCase {
    private final ProjectRepository projectRepository;

    public UpdateProjectTicketTypesUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project execute(long projectId, java.util.UUID requesterId, java.util.List<com.mossman.domain.entities.TicketType> newTicketTypes) {
        Project current = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));
        com.mossman.domain.auth.PermissionChecker.requireProjectEditor(current, requesterId);

        long distinctKeys = newTicketTypes.stream().map(t -> t.key().toLowerCase()).distinct().count();
        if (distinctKeys < newTicketTypes.size()) {
            throw new IllegalArgumentException("Ticket type key must be unique for each project.");
        }
        newTicketTypes.forEach(t -> {
            EntityValidator.requireValidKey("ticket type key", t.key());
            EntityValidator.requireValidDisplayName("ticket type display name", t.displayName());
            TextColorValidator.requireValid("textColor", t.textColor());
        });

        Project updated = Project.builder()
                .id(current.getId())
                .ticketPrefix(current.getTicketPrefix())
                .name(current.getName())
                .description(current.getDescription())
                .iconTexture(current.getIconTexture())
                .textColor(current.getTextColor())
                .statuses(current.getStatuses())
                .ticketTypes(newTicketTypes)
                .relationshipTypes(current.getRelationshipTypes())
                .members(current.getMembers())
                .externalUserPermission(current.getExternalUserPermission())
                .build();

        return projectRepository.save(updated);
    }
}
