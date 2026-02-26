package com.mossman.domain.usecases;

import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.validation.EntityValidator;
import com.mossman.domain.validation.TextColorValidator;

public class UpdateProjectRelationshipTypesUseCase {
    private final ProjectRepository projectRepository;

    public UpdateProjectRelationshipTypesUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project execute(long projectId, java.util.UUID requesterId, java.util.List<com.mossman.domain.entities.RelationshipType> newRelTypes) {
        Project current = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));
        com.mossman.domain.auth.PermissionChecker.requireProjectEditor(current, requesterId);

        long distinctKeys = newRelTypes.stream().map(r -> r.key().toLowerCase()).distinct().count();
        if (distinctKeys < newRelTypes.size()) {
            throw new IllegalArgumentException("Relationship type key must be unique for each project.");
        }
        newRelTypes.forEach(r -> {
            EntityValidator.requireValidKey("relationship type key", r.key());
            EntityValidator.requireValidDisplayName("relationship type display name", r.displayName());
            EntityValidator.requireValidRelTypeDescription("sourceToTargetDescription", r.sourceToTargetDescription());
            EntityValidator.requireValidRelTypeDescription("targetToSourceDescription", r.targetToSourceDescription());
            TextColorValidator.requireValid("textColor", r.textColor());
        });

        Project updated = Project.builder()
                .id(current.getId())
                .ticketPrefix(current.getTicketPrefix())
                .name(current.getName())
                .description(current.getDescription())
                .iconTexture(current.getIconTexture())
                .textColor(current.getTextColor())
                .statuses(current.getStatuses())
                .ticketTypes(current.getTicketTypes())
                .relationshipTypes(newRelTypes)
                .members(current.getMembers())
                .externalUserPermission(current.getExternalUserPermission())
                .build();

        return projectRepository.save(updated);
    }
}
