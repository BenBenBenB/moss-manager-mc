package com.mossman.core.support;

import com.mossman.core.model.Project;
import com.mossman.core.model.Role;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

public final class Projects {

    private Projects() {}

    public static Project seeded(UUID owner) {
        return seeded("test", "Test", owner);
    }

    public static Project seeded(String id, String name, UUID owner) {
        return Project.create(id, name, owner);
    }

    public static UUID roleIdByName(Project project, String roleName) {
        for (Role r : project.roles()) {
            if (r.name().equals(roleName)) return r.id();
        }
        throw new NoSuchElementException("no role named " + roleName);
    }

    public static Project assignByName(Project project, UUID player, String roleName) {
        UUID roleId = roleIdByName(project, roleName);
        Map<UUID, Set<UUID>> next = new HashMap<>(project.memberRoles());
        Set<UUID> existing = next.getOrDefault(player, Set.of());
        Set<UUID> merged = new HashSet<>(existing);
        merged.add(roleId);
        next.put(player, Set.copyOf(merged));
        return project.withMemberRoles(next);
    }

    public static Project unassignAll(Project project, UUID player) {
        Map<UUID, Set<UUID>> next = new HashMap<>(project.memberRoles());
        next.remove(player);
        return project.withMemberRoles(next);
    }

    public static Project allowingNonMembers(Project project, boolean allow) {
        return project.withAllowNonMembers(allow);
    }
}
