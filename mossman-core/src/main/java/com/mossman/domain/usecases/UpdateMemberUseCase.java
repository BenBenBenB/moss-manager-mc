package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.validation.EntityValidator;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateMemberUseCase {
    private final ProjectRepository projectRepository;

    public UpdateMemberUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project execute(long projectId, UUID requesterId, UUID targetMemberId, Map<String, String> patch) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + projectId));

        PermissionChecker.requireProjectAdmin(project, requesterId);

        List<Member> members = project.getMembers();
        Member current = members.stream()
                .filter(m -> m.uuid().equals(targetMemberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + targetMemberId));

        // Patching logic
        String username   = patch.getOrDefault("username",   current.username());
        String title      = patch.getOrDefault("title",      current.title() != null ? current.title() : "");
        Permission perm   = patch.containsKey("permission")
                ? Permission.valueOf(patch.get("permission").toUpperCase())
                : current.permission();

        // Permission ceiling check
        Permission requesterPerm = PermissionChecker.getEffectivePermission(project, requesterId);
        if (requesterPerm == Permission.ADMIN) {
            // Admin can update another ADMIN's details (username, title), but NOT their permission level
            if (current.permission() == Permission.ADMIN) {
                if (perm != Permission.ADMIN) {
                    throw new SecurityException("Admins cannot change the permission level of other ADMINs.");
                }
            }
            // Admin still cannot modify the OWNER at all
            if (current.permission() == Permission.OWNER) {
                throw new SecurityException("Admins cannot modify the OWNER.");
            }
            // Admin cannot promote anyone to ADMIN or OWNER
            if (perm == Permission.ADMIN || perm == Permission.OWNER) {
                if (current.permission() != perm) { 
                    throw new SecurityException("Admins can only assign up to EDITOR level.");
                }
            }
        }

        EntityValidator.requireValidMemberTitle(title);

        Member updatedMember = new Member(current.id(), current.uuid(), username, title, perm);

        List<Member> updatedMembers = members.stream()
                .map(m -> m.uuid().equals(targetMemberId) ? updatedMember : m)
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
