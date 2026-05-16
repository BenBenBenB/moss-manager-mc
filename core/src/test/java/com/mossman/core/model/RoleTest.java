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
                () -> new Role(null, "x", Set.of(), 0));
    }

    @Test
    void rejectsBlankName() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new Role(id, "", Set.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new Role(id, "   ", Set.of(), 0));
    }

    @Test
    void permissionsAreDefensivelyCopied() {
        Set<Permission> mutable = new HashSet<>(Set.of(Permission.VIEW_PROJECT));
        Role r = new Role(UUID.randomUUID(), "n", mutable, 0);
        mutable.add(Permission.EDIT_PROJECT);
        assertEquals(Set.of(Permission.VIEW_PROJECT), r.permissions());
    }

    @Test
    void permissionsAreUnmodifiable() {
        Role r = new Role(UUID.randomUUID(), "n", Set.of(Permission.VIEW_PROJECT), 0);
        assertThrows(UnsupportedOperationException.class,
                () -> r.permissions().add(Permission.EDIT_PROJECT));
    }

    @Test
    void nullPermissionsBecomeEmpty() {
        Role r = new Role(UUID.randomUUID(), "n", null, 0);
        assertTrue(r.permissions().isEmpty());
    }

    @Test
    void withMethodsProduceNewInstance() {
        Role r = new Role(UUID.randomUUID(), "Old", Set.of(Permission.VIEW_PROJECT), 0xFF0000);
        assertEquals("New", r.withName("New").name());
        assertEquals(Set.of(Permission.EDIT_PROJECT),
                r.withPermissions(Set.of(Permission.EDIT_PROJECT)).permissions());
        assertEquals(0x00FF00, r.withColor(0x00FF00).color());
    }
}
