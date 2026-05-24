package com.mossman.core.model;

import com.mossman.core.permission.Permission;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new Role(null, "x", Set.of(), Set.of(), 0));
    }

    @Test
    void rejectsBlankName() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new Role(id, "", Set.of(), Set.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new Role(id, "   ", Set.of(), Set.of(), 0));
    }

    @Test
    void grantsAreDefensivelyCopied() {
        Set<Permission> mutable = new HashSet<>(Set.of(Permission.VIEW_PROJECT));
        Role r = new Role(UUID.randomUUID(), "n", mutable, Set.of(), 0);
        mutable.add(Permission.EDIT_PROJECT);
        assertEquals(Set.of(Permission.VIEW_PROJECT), r.grants());
    }

    @Test
    void grantsAreUnmodifiable() {
        Role r = new Role(UUID.randomUUID(), "n", Set.of(Permission.VIEW_PROJECT), Set.of(), 0);
        assertThrows(UnsupportedOperationException.class,
                () -> r.grants().add(Permission.EDIT_PROJECT));
    }

    @Test
    void nullGrantsBecomeEmpty() {
        Role r = new Role(UUID.randomUUID(), "n", null, null, 0);
        assertTrue(r.grants().isEmpty());
        assertTrue(r.denials().isEmpty());
    }

    @Test
    void grantsAndDenialsMayNotOverlap() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new Role(id, "n",
                        Set.of(Permission.VIEW_PROJECT, Permission.EDIT_PROJECT),
                        Set.of(Permission.EDIT_PROJECT),
                        0));
    }

    @Test
    void withMethodsProduceNewInstance() {
        Role r = new Role(UUID.randomUUID(), "Old",
                Set.of(Permission.VIEW_PROJECT), Set.of(), 0xFF0000);
        assertEquals("New", r.withName("New").name());
        assertEquals(Set.of(Permission.EDIT_PROJECT),
                r.withGrants(Set.of(Permission.EDIT_PROJECT)).grants());
        assertEquals(Set.of(Permission.DELETE_TICKETS),
                r.withDenials(Set.of(Permission.DELETE_TICKETS)).denials());
        assertEquals(0x00FF00, r.withColor(0x00FF00).color());
    }
}
