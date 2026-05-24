package com.mossman.core.model;

import com.mossman.core.permission.Permission;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Role(UUID id, String name, Set<Permission> grants, Set<Permission> denials, int color) {

    public Role {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("name is blank");
        grants = grants == null ? Set.of() : Set.copyOf(grants);
        denials = denials == null ? Set.of() : Set.copyOf(denials);
        Set<Permission> overlap = new HashSet<>(grants);
        overlap.retainAll(denials);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException(
                    "role '" + name + "' grants and denials overlap: " + overlap);
        }
    }

    public Role withName(String newName) {
        return new Role(id, newName, grants, denials, color);
    }

    public Role withGrants(Set<Permission> newGrants) {
        return new Role(id, name, newGrants, denials, color);
    }

    public Role withDenials(Set<Permission> newDenials) {
        return new Role(id, name, grants, newDenials, color);
    }

    public Role withColor(int newColor) {
        return new Role(id, name, grants, denials, newColor);
    }
}
