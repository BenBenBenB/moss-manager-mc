package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

public class TransferOwnershipUseCase {
    private final ProjectRepository projectRepository;

    public TransferOwnershipUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project execute(long projectId, UUID requesterId, UUID newOwnerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));

        // Only the OWNER can transfer ownership
        Member currentOwner = project.getOwner()
                .orElseThrow(() -> new IllegalStateException("Project has no owner!"));
        
        if (!currentOwner.uuid().equals(requesterId)) {
            throw new SecurityException("Only the project owner can transfer ownership.");
        }

        // New owner must be a member
        Member newOwnerMemberTemplate = project.getMembers().stream()
                .filter(m -> m.uuid().equals(newOwnerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("New owner must be an existing project member."));

        List<Member> updatedMembers = project.getMembers().stream()
                .map(m -> {
                    if (m.uuid().equals(requesterId)) {
                        // Old owner becomes ADMIN
                        return new Member(m.id(), m.uuid(), m.username(), m.title(), Permission.ADMIN);
                    } else if (m.uuid().equals(newOwnerId)) {
                        // Target becomes OWNER
                        return new Member(m.id(), m.uuid(), m.username(), m.title(), Permission.OWNER);
                    }
                    return m;
                })
                .collect(Collectors.toList());

        Project updatedProject = Project.builder()
                .id(project.getId())
                .ticketPrefix(project.getTicketPrefix())
                .name(project.getName())
                .description(project.getDescription())
                .iconTexture(project.getIconTexture())
                .statuses(project.getStatuses())
                .ticketTypes(project.getTicketTypes())
                .relationshipTypes(project.getRelationshipTypes())
                .members(updatedMembers)
                .externalUserPermission(project.getExternalUserPermission())
                .textColor(project.getTextColor())
                .build();

        return projectRepository.save(updatedProject);
    }
}
