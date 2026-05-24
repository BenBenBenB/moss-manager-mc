package com.mossman.core.permission;

import com.mossman.core.model.Project;
import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import com.mossman.core.usecase.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionEvaluatorTest {

    @Test
    void seededOwnerHasAllAdminPermissions() {
        Project p = Projects.seeded(Players.OWNER);
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.OWNER);
        // Admin grants the union of Viewer + Editor + admin-specific perms.
        assertTrue(eff.contains(Permission.EDIT_PROJECT));
        assertTrue(eff.contains(Permission.MANAGE_ROLES));
        assertTrue(eff.contains(Permission.DELETE_TICKETS));
        assertTrue(eff.contains(Permission.CREATE_TICKETS));
        assertTrue(eff.contains(Permission.VIEW_PROJECT));
    }

    @Test
    void ownerStrippedOfAllRolesFallsBackToDefault_allowNonMembersTrue() {
        Project base = Projects.seeded(Players.OWNER);
        Project p = Projects.unassignAll(base, Players.OWNER);
        assertTrue(p.allowNonMembers());

        Set<Permission> eff = PermissionEvaluator.effective(p, Players.OWNER);
        // Default role grants only VIEW perms in our defaults.
        assertEquals(Set.of(Permission.VIEW_PROJECT, Permission.VIEW_TICKETS), eff);
        // owner has lost graded permissions; gradeable ADMIN actions are denied.
        assertFalse(eff.contains(Permission.EDIT_PROJECT));
        assertFalse(eff.contains(Permission.MANAGE_ROLES));
    }

    @Test
    void ownerStrippedOfAllRoles_allowNonMembersFalse_getsNothing() {
        Project base = Projects.seeded(Players.OWNER);
        Project p = Projects.allowingNonMembers(Projects.unassignAll(base, Players.OWNER), false);
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.OWNER);
        assertTrue(eff.isEmpty());
    }

    @Test
    void nonMember_allowNonMembersTrue_getsDefaultRolePerms() {
        Project p = Projects.seeded(Players.OWNER);
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.STRANGER);
        assertEquals(Set.of(Permission.VIEW_PROJECT, Permission.VIEW_TICKETS), eff);
    }

    @Test
    void nonMember_allowNonMembersFalse_getsNothing() {
        Project p = Projects.allowingNonMembers(Projects.seeded(Players.OWNER), false);
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.STRANGER);
        assertTrue(eff.isEmpty());
    }

    @Test
    void memberWithViewerRole_unionDefaultAndViewer() {
        Project base = Projects.seeded(Players.OWNER);
        Project p = Projects.assignByName(base, Players.ALICE, "Viewer");
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.ALICE);
        assertEquals(Set.of(Permission.VIEW_PROJECT, Permission.VIEW_TICKETS), eff);
        assertFalse(eff.contains(Permission.CREATE_TICKETS));
    }

    @Test
    void memberWithEditorRole_unionDefaultAndEditor() {
        Project base = Projects.seeded(Players.OWNER);
        Project p = Projects.assignByName(base, Players.ALICE, "Editor");
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.ALICE);
        assertTrue(eff.contains(Permission.CREATE_TICKETS));
        assertTrue(eff.contains(Permission.EDIT_TICKETS));
        assertTrue(eff.contains(Permission.CHANGE_TICKET_STATUS));
        assertTrue(eff.contains(Permission.ASSIGN_TICKETS));
        assertFalse(eff.contains(Permission.EDIT_PROJECT));
        assertFalse(eff.contains(Permission.DELETE_TICKETS));
    }

    @Test
    void memberWithMultipleRoles_unionIsCorrect() {
        Project base = Projects.seeded(Players.OWNER);
        Project p = Projects.assignByName(
                Projects.assignByName(base, Players.ALICE, "Viewer"),
                Players.ALICE, "Editor");
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.ALICE);
        // Editor's perms are a superset of viewer's; union is just editor's perms.
        assertTrue(eff.contains(Permission.CREATE_TICKETS));
        assertTrue(eff.contains(Permission.VIEW_PROJECT));
    }

    @Test
    void defaultRoleAppliesToMembersEvenWhenAllowNonMembersFalse() {
        Project base = Projects.seeded(Players.OWNER);
        Project p = Projects.allowingNonMembers(
                Projects.assignByName(base, Players.ALICE, "Editor"),
                false);
        Set<Permission> eff = PermissionEvaluator.effective(p, Players.ALICE);
        // Default role still contributes VIEW_PROJECT + VIEW_TICKETS to members.
        assertTrue(eff.contains(Permission.VIEW_PROJECT));
        assertTrue(eff.contains(Permission.VIEW_TICKETS));
        assertTrue(eff.contains(Permission.CREATE_TICKETS));
    }

    @Test
    void hasReturnsBoolFromEffective() {
        Project p = Projects.assignByName(
                Projects.seeded(Players.OWNER), Players.ALICE, "Editor");
        assertTrue(PermissionEvaluator.has(p, Players.ALICE, Permission.CREATE_TICKETS));
        assertFalse(PermissionEvaluator.has(p, Players.ALICE, Permission.EDIT_PROJECT));
    }

    @Test
    void requireThrowsWithProjectActorAndRequired() {
        Project p = Projects.seeded(Players.OWNER);
        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> PermissionEvaluator.require(p, Players.STRANGER, Permission.EDIT_PROJECT));
        assertEquals(p.id(), ex.projectId());
        assertEquals(Players.STRANGER, ex.actor());
        assertEquals(Permission.EDIT_PROJECT, ex.requiredPermission().orElseThrow());
        assertFalse(ex.isOwnerOnly());
    }

    @Test
    void requireSucceedsWhenPermissionGranted() {
        Project p = Projects.seeded(Players.OWNER);
        assertDoesNotThrow(() -> PermissionEvaluator.require(p, Players.OWNER, Permission.MANAGE_ROLES));
    }

    @Test
    void higherIndexRoleDenialOverridesLowerGrant() {
        // Build a project where the Default role grants EDIT_TICKETS but a
        // higher-index "Restricted" role (appended at the tail) denies it.
        // The evaluator walks low→high, so the denial wins for members of
        // Restricted.
        Project base = Projects.seeded(Players.OWNER);

        com.mossman.core.model.Role defaultRole = base.defaultRole().withGrants(
                java.util.Set.of(Permission.VIEW_PROJECT, Permission.VIEW_TICKETS, Permission.EDIT_TICKETS));
        com.mossman.core.model.Role restricted = new com.mossman.core.model.Role(
                java.util.UUID.randomUUID(), "Restricted",
                java.util.Set.of(),
                java.util.Set.of(Permission.EDIT_TICKETS),
                0);
        java.util.List<com.mossman.core.model.Role> roles = new java.util.ArrayList<>(base.roles());
        roles.set(0, defaultRole);
        roles.add(restricted);

        java.util.Map<java.util.UUID, java.util.Set<java.util.UUID>> mr = new java.util.HashMap<>(base.memberRoles());
        mr.put(Players.ALICE, java.util.Set.of(restricted.id()));
        Project p = base.withRoles(roles).withMemberRoles(mr);

        Set<Permission> eff = PermissionEvaluator.effective(p, Players.ALICE);
        assertTrue(eff.contains(Permission.VIEW_PROJECT));
        assertFalse(eff.contains(Permission.EDIT_TICKETS),
                "Restricted's denial of EDIT_TICKETS should override Default's grant");
    }
}
