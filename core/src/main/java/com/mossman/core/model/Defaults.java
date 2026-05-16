package com.mossman.core.model;

import com.mossman.core.permission.Permission;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class Defaults {

    private Defaults() {}

    static final int COLOR_GRAY  = 0x9E9E9E;
    static final int COLOR_BLUE  = 0x3F87FF;
    static final int COLOR_GREEN = 0x2ECC71;
    static final int COLOR_RED   = 0xE74C3C;
    static final int COLOR_AMBER = 0xF1C40F;
    static final int COLOR_WHITE = 0xFFFFFF;

    static Project newProject(String id, String name, UUID owner) {
        UUID adminId   = UUID.randomUUID();
        UUID editorId  = UUID.randomUUID();
        UUID viewerId  = UUID.randomUUID();
        UUID defaultId = UUID.randomUUID();

        Set<Permission> viewerPerms = Set.of(
                Permission.VIEW_PROJECT,
                Permission.VIEW_TICKETS);
        Set<Permission> editorPerms = EnumSet.copyOf(viewerPerms);
        editorPerms.addAll(Set.of(
                Permission.CREATE_TICKETS,
                Permission.EDIT_TICKETS,
                Permission.ASSIGN_TICKETS,
                Permission.CHANGE_TICKET_STATUS));
        Set<Permission> adminPerms = EnumSet.copyOf(editorPerms);
        adminPerms.addAll(Set.of(
                Permission.EDIT_PROJECT,
                Permission.MANAGE_ROLES,
                Permission.DELETE_TICKETS));

        Role admin   = new Role(adminId,   "Admin",    adminPerms,   COLOR_RED);
        Role editor  = new Role(editorId,  "Editor",   editorPerms,  COLOR_BLUE);
        Role viewer  = new Role(viewerId,  "Viewer",   viewerPerms,  COLOR_GRAY);
        Role defRole = new Role(defaultId, "Everyone", viewerPerms,  COLOR_GRAY);

        List<Role> roles = List.of(admin, editor, viewer, defRole);

        TicketStatus todo       = new TicketStatus(UUID.randomUUID(), "TODO",        COLOR_WHITE, COLOR_GRAY);
        TicketStatus inProgress = new TicketStatus(UUID.randomUUID(), "In Progress", COLOR_WHITE, COLOR_BLUE);
        TicketStatus done       = new TicketStatus(UUID.randomUUID(), "Done",        COLOR_WHITE, COLOR_GREEN);
        List<TicketStatus> statuses = List.of(todo, inProgress, done);

        TicketType bug     = new TicketType(UUID.randomUUID(), "Bug",     COLOR_WHITE, COLOR_RED);
        TicketType feature = new TicketType(UUID.randomUUID(), "Feature", COLOR_WHITE, COLOR_BLUE);
        TicketType task    = new TicketType(UUID.randomUUID(), "Task",    COLOR_WHITE, COLOR_GRAY);
        TicketType chore   = new TicketType(UUID.randomUUID(), "Chore",   COLOR_WHITE, COLOR_AMBER);
        List<TicketType> types = List.of(bug, feature, task, chore);

        Map<UUID, Set<UUID>> memberRoles = Map.of(owner, Set.of(adminId));

        return new Project(
                id,
                name,
                owner,
                defaultId,
                true,
                roles,
                memberRoles,
                statuses,
                types,
                List.of());
    }
}
