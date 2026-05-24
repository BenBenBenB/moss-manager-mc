package com.mossman.core.model;

import com.mossman.core.support.Players;
import com.mossman.core.support.Projects;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectTest {

    @Test
    void rejectsNullAndBlankRequired() {
        Project p = Projects.seeded(Players.OWNER);
        // direct canonical-constructor invocations of nulls and blanks
        assertThrows(NullPointerException.class,
                () -> new Project(null, "n", Players.OWNER, p.defaultRoleId(), true,
                        p.roles(), p.memberRoles(), p.statuses(), p.types(), p.tickets(), p.nextTicketNumber()));
        assertThrows(IllegalArgumentException.class,
                () -> new Project(" ", "n", Players.OWNER, p.defaultRoleId(), true,
                        p.roles(), p.memberRoles(), p.statuses(), p.types(), p.tickets(), p.nextTicketNumber()));
        assertThrows(NullPointerException.class,
                () -> new Project("id", null, Players.OWNER, p.defaultRoleId(), true,
                        p.roles(), p.memberRoles(), p.statuses(), p.types(), p.tickets(), p.nextTicketNumber()));
        assertThrows(IllegalArgumentException.class,
                () -> new Project("id", " ", Players.OWNER, p.defaultRoleId(), true,
                        p.roles(), p.memberRoles(), p.statuses(), p.types(), p.tickets(), p.nextTicketNumber()));
        assertThrows(NullPointerException.class,
                () -> new Project("id", "n", null, p.defaultRoleId(), true,
                        p.roles(), p.memberRoles(), p.statuses(), p.types(), p.tickets(), p.nextTicketNumber()));
        assertThrows(NullPointerException.class,
                () -> new Project("id", "n", Players.OWNER, null, true,
                        p.roles(), p.memberRoles(), p.statuses(), p.types(), p.tickets(), p.nextTicketNumber()));
    }

    @Test
    void collectionsAreUnmodifiableAndDefensivelyCopied() {
        Project p = Projects.seeded(Players.OWNER);

        assertThrows(UnsupportedOperationException.class, () -> p.roles().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> p.memberRoles().put(Players.STRANGER, Set.of()));
        assertThrows(UnsupportedOperationException.class, () -> p.statuses().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.types().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.tickets().clear());
        for (Set<UUID> s : p.memberRoles().values()) {
            assertThrows(UnsupportedOperationException.class, () -> s.add(UUID.randomUUID()));
        }
    }

    @Test
    void defaultRoleMustBeFirstInList() {
        Project p = Projects.seeded(Players.OWNER);
        List<Role> bad = new ArrayList<>(p.roles());
        // move default off the top
        Role def = bad.remove(0);
        bad.add(def);
        assertThrows(IllegalArgumentException.class, () -> p.withRoles(bad));
    }

    @Test
    void rejectsEmptyRoles() {
        Project p = Projects.seeded(Players.OWNER);
        assertThrows(IllegalArgumentException.class, () -> p.withRoles(List.of()));
    }

    @Test
    void rejectsMemberRolesReferencingUnknownRole() {
        Project p = Projects.seeded(Players.OWNER);
        Map<UUID, Set<UUID>> bad = new HashMap<>(p.memberRoles());
        bad.put(Players.ALICE, Set.of(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> p.withMemberRoles(bad));
    }

    @Test
    void rejectsMemberRolesReferencingDefaultRole() {
        Project p = Projects.seeded(Players.OWNER);
        Map<UUID, Set<UUID>> bad = new HashMap<>(p.memberRoles());
        bad.put(Players.ALICE, Set.of(p.defaultRoleId()));
        assertThrows(IllegalArgumentException.class, () -> p.withMemberRoles(bad));
    }

    @Test
    void rejectsEmptyMemberRoleSet() {
        Project p = Projects.seeded(Players.OWNER);
        Map<UUID, Set<UUID>> bad = new HashMap<>(p.memberRoles());
        bad.put(Players.ALICE, Set.of());
        assertThrows(IllegalArgumentException.class, () -> p.withMemberRoles(bad));
    }

    @Test
    void rejectsTicketsReferencingUnknownStatusOrType() {
        Project p = Projects.seeded(Players.OWNER);
        UUID badStatus = UUID.randomUUID();
        UUID realType = p.types().get(0).id();
        UUID realStatus = p.statuses().get(0).id();
        UUID badType = UUID.randomUUID();

        Ticket badStatusTicket = new Ticket(UUID.randomUUID(), 1, "t", "", null, badStatus, realType, 0L);
        Ticket badTypeTicket = new Ticket(UUID.randomUUID(), 1, "t", "", null, realStatus, badType, 0L);

        assertThrows(IllegalArgumentException.class, () -> p.withTickets(List.of(badStatusTicket)));
        assertThrows(IllegalArgumentException.class, () -> p.withTickets(List.of(badTypeTicket)));
    }

    @Test
    void seededProjectHasOwnerAssignedToAdmin() {
        Project p = Projects.seeded(Players.OWNER);
        UUID adminId = Projects.roleIdByName(p, "Admin");
        assertEquals(Set.of(adminId), p.memberRoles().get(Players.OWNER));
    }

    @Test
    void seededProjectHasExpectedDefaultsShape() {
        Project p = Projects.seeded(Players.OWNER);
        // four roles: Default, Viewer, Editor, Admin (default at index 0)
        assertEquals(4, p.roles().size());
        assertEquals("Default", p.defaultRole().name());
        // three statuses, four types
        assertEquals(3, p.statuses().size());
        assertEquals(4, p.types().size());
        assertTrue(p.allowNonMembers());
        assertTrue(p.tickets().isEmpty());
    }

    @Test
    void findHelpersWorkAndReturnEmptyOnMiss() {
        Project p = Projects.seeded(Players.OWNER);
        assertTrue(p.findRole(p.defaultRoleId()).isPresent());
        assertTrue(p.findStatus(p.statuses().get(0).id()).isPresent());
        assertTrue(p.findType(p.types().get(0).id()).isPresent());
        assertTrue(p.findRole(UUID.randomUUID()).isEmpty());
        assertTrue(p.findStatus(UUID.randomUUID()).isEmpty());
        assertTrue(p.findType(UUID.randomUUID()).isEmpty());
        assertTrue(p.findTicket(UUID.randomUUID()).isEmpty());
    }

    @Test
    void addReplaceRemoveTicket() {
        Project p = Projects.seeded(Players.OWNER);
        UUID statusId = p.statuses().get(0).id();
        UUID typeId = p.types().get(0).id();
        Ticket t = new Ticket(UUID.randomUUID(), 1,"Hi", "", null, statusId, typeId, 0L);

        Project p2 = p.addTicket(t);
        assertEquals(1, p2.tickets().size());
        assertEquals(t, p2.findTicket(t.id()).orElseThrow());

        Ticket renamed = t.withTitle("Hi 2");
        Project p3 = p2.replaceTicket(renamed);
        assertEquals("Hi 2", p3.findTicket(t.id()).orElseThrow().title());

        Project p4 = p3.removeTicket(t.id());
        assertTrue(p4.tickets().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> p4.removeTicket(t.id()));
        assertThrows(IllegalArgumentException.class, () -> p4.replaceTicket(t));
    }

    @Test
    void rejectsDuplicateRoleIds() {
        Project p = Projects.seeded(Players.OWNER);
        UUID adminId = Projects.roleIdByName(p, "Admin");
        List<Role> dupe = new ArrayList<>(p.roles());
        // replace the editor with another role using admin's id
        dupe.set(2, new Role(adminId, "ImposterEditor", Set.of(), Set.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> p.withRoles(dupe));
    }
}
