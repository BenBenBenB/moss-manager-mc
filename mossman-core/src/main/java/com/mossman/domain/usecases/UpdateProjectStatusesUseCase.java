package com.mossman.domain.usecases;

import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.validation.EntityValidator;
import com.mossman.domain.validation.TextColorValidator;

public class UpdateProjectStatusesUseCase {
    private final ProjectRepository projectRepository;

    public UpdateProjectStatusesUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project execute(long projectId, java.util.UUID requesterId, java.util.List<com.mossman.domain.entities.Status> newStatuses) {
        Project current = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));
        com.mossman.domain.auth.PermissionChecker.requireProjectEditor(current, requesterId);

        long distinctKeys = newStatuses.stream().map(s -> s.key().toLowerCase()).distinct().count();
        if (distinctKeys < newStatuses.size()) {
            throw new IllegalArgumentException("Status key must be unique for each project.");
        }
        newStatuses.forEach(s -> {
            EntityValidator.requireValidKey("status key", s.key());
            EntityValidator.requireValidDisplayName("status display name", s.displayName());
            TextColorValidator.requireValid("textColor", s.textColor());
        });

        Project updated = Project.builder()
                .id(current.getId())
                .ticketPrefix(current.getTicketPrefix())
                .name(current.getName())
                .description(current.getDescription())
                .iconTexture(current.getIconTexture())
                .textColor(current.getTextColor())
                .statuses(newStatuses)
                .ticketTypes(current.getTicketTypes())
                .relationshipTypes(current.getRelationshipTypes())
                .members(current.getMembers())
                .externalUserPermission(current.getExternalUserPermission())
                .build();

        return projectRepository.save(updated);
    }
}
