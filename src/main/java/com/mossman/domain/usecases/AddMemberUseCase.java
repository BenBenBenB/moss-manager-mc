package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.auth.PermissionChecker;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class AddMemberUseCase {
    private final ProjectRepository projectRepository;

    public AddMemberUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project execute(long projectId, UUID requesterId, Member newMember) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));

        // Security check: Must be ADMIN or OWNER
        PermissionChecker.requireProjectAdmin(project, requesterId);

        // Specific rule: ADMINs cannot add members with ADMIN or OWNER permission
        Permission requesterPerm = PermissionChecker.getEffectivePermission(project, requesterId);
        if (requesterPerm == Permission.ADMIN) {
            if (newMember.permission() == Permission.ADMIN || newMember.permission() == Permission.OWNER) {
                throw new SecurityException("Admins can only add members up to EDITOR level.");
            }
        }

        boolean alreadyMember = project.getMembers().stream().anyMatch(m -> m.uuid().equals(newMember.uuid()));
        if (alreadyMember) {
            throw new IllegalArgumentException("Player is already a member of this project");
        }

        // Add member logic
        List<Member> updatedMembers = new ArrayList<>(project.getMembers());
        updatedMembers.add(newMember);

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
