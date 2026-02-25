package com.mossman.domain.usecases;

import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.validation.EntityValidator;
import com.mossman.domain.validation.TextColorValidator;

import java.util.Map;

/**
 * Applies a partial update (patch) to a project using a String->String map of field values.
 * Only fields present in the patch map are changed; all others are left as-is.
 * The caller (command layer) is responsible for parsing SNBT into the map.
 *
 * Patchable fields: ticketPrefix, name, description, iconTexture, textColor, externalUserPermission
 */
public class UpdateProjectUseCase {
    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public UpdateProjectUseCase(ProjectRepository projectRepository, DomainEventBus eventBus) {
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    /**
     * @param projectId   DB id of the project to patch
     * @param requesterId UUID of the user requesting the update
     * @param patch       map of field name → new string value (from SNBT)
     * @return the updated Project
     * @throws IllegalArgumentException if the project does not exist
     * @throws SecurityException if the requester does not have ADMIN permission
     */
    public Project execute(long projectId, java.util.UUID requesterId, Map<String, String> patch) {
        Project current = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));

        com.mossman.domain.auth.PermissionChecker.requireProjectAdmin(current, requesterId);

        if (patch.containsKey("textColor")) TextColorValidator.requireValid("textColor", patch.get("textColor"));
        if (patch.containsKey("ticketPrefix")) EntityValidator.requireValidTicketPrefix(patch.get("ticketPrefix"));
        if (patch.containsKey("name"))         EntityValidator.requireValidProjectName(patch.get("name"));
        if (patch.containsKey("description"))  EntityValidator.requireValidProjectDescription(patch.get("description"));
        if (patch.containsKey("iconTexture"))  EntityValidator.requireValidIconTexture(patch.get("iconTexture"));

        String name                   = patch.getOrDefault("name",        current.getName());
        String description            = patch.getOrDefault("description", current.getDescription() != null ? current.getDescription() : "");
        String iconTexture            = patch.getOrDefault("iconTexture",  current.getIconTexture() != null ? current.getIconTexture() : "");
        String textColor              = patch.getOrDefault("textColor",    current.getTextColor() != null ? current.getTextColor() : "");
        Permission externalPermission = patch.containsKey("externalUserPermission")
                ? Permission.valueOf(patch.get("externalUserPermission").toUpperCase())
                : current.getExternalUserPermission();

        String ticketPrefix           = patch.containsKey("ticketPrefix")
                ? patch.get("ticketPrefix").toUpperCase()
                : current.getTicketPrefix();

        Project updated = Project.builder()
                .id(current.getId())
                .ticketPrefix(ticketPrefix)
                .name(name)
                .description(description)
                .iconTexture(iconTexture)
                .textColor(textColor)
                .statuses(current.getStatuses())
                .ticketTypes(current.getTicketTypes())
                .relationshipTypes(current.getRelationshipTypes())
                .members(current.getMembers())
                .externalUserPermission(externalPermission)
                .build();

        return projectRepository.save(updated);
    }
}
