package com.mossman.core.model;

import com.mossman.core.permission.Permission;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Role(UUID id, String name, Set<Permission> permissions, int color) {

    public Role {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("name is blank");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public Role withName(String newName) {
        return new Role(id, newName, permissions, color);
    }

    public Role withPermissions(Set<Permission> newPermissions) {
        return new Role(id, name, newPermissions, color);
    }

    public Role withColor(int newColor) {
        return new Role(id, name, permissions, newColor);
    }
}
