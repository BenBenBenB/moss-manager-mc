package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.auth.PermissionChecker;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveMemberUseCase {
    private final ProjectRepository projectRepository;

    public RemoveMemberUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project execute(long projectId, UUID requesterId, UUID targetMemberId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));

        PermissionChecker.requireProjectAdmin(project, requesterId);

        Member target = project.getMembers().stream()
                .filter(m -> m.uuid().equals(targetMemberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + targetMemberId));

        // Protection: Can't remove the OWNER
        if (target.permission() == Permission.OWNER) {
            throw new SecurityException("The project owner cannot be removed. Transfer ownership first.");
        }

        // Admin ceiling: Admins can't remove other ADMINs
        Permission requesterPerm = PermissionChecker.getEffectivePermission(project, requesterId);
        if (requesterPerm == Permission.ADMIN && target.permission() == Permission.ADMIN) {
            throw new SecurityException("Admins cannot remove other admins.");
        }

        List<Member> updatedMembers = project.getMembers().stream()
                .filter(m -> !m.uuid().equals(targetMemberId))
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
