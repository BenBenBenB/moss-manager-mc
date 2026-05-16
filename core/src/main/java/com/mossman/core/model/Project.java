package com.mossman.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record Project(
        String id,
        String name,
        UUID ownerUuid,
        UUID defaultRoleId,
        boolean allowNonMembers,
        List<Role> roles,
        Map<UUID, Set<UUID>> memberRoles,
        List<TicketStatus> statuses,
        List<TicketType> types,
        List<Ticket> tickets
) {

    public Project {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(ownerUuid, "ownerUuid");
        Objects.requireNonNull(defaultRoleId, "defaultRoleId");
        if (id.isBlank()) throw new IllegalArgumentException("id is blank");
        if (name.isBlank()) throw new IllegalArgumentException("name is blank");

        roles = roles == null ? List.of() : List.copyOf(roles);
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        types = types == null ? List.of() : List.copyOf(types);
        tickets = tickets == null ? List.of() : List.copyOf(tickets);

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("project must have at least the default role");
        }
        Role last = roles.get(roles.size() - 1);
        if (!last.id().equals(defaultRoleId)) {
            throw new IllegalArgumentException("default role must be the last entry of roles");
        }

        Set<UUID> nonDefaultRoleIds = new HashSet<>();
        for (int i = 0; i < roles.size() - 1; i++) {
            nonDefaultRoleIds.add(roles.get(i).id());
        }
        Set<UUID> allRoleIds = new HashSet<>(nonDefaultRoleIds);
        allRoleIds.add(defaultRoleId);
        if (allRoleIds.size() != roles.size()) {
            throw new IllegalArgumentException("roles contains duplicate ids");
        }

        if (memberRoles == null) {
            memberRoles = Map.of();
        } else {
            Map<UUID, Set<UUID>> copy = new LinkedHashMap<>();
            for (Map.Entry<UUID, Set<UUID>> e : memberRoles.entrySet()) {
                Objects.requireNonNull(e.getKey(), "memberRoles key");
                Objects.requireNonNull(e.getValue(), "memberRoles value");
                Set<UUID> assigned = Set.copyOf(e.getValue());
                if (assigned.isEmpty()) {
                    throw new IllegalArgumentException("memberRoles entry for " + e.getKey() + " has no roles");
                }
                for (UUID roleId : assigned) {
                    if (!nonDefaultRoleIds.contains(roleId)) {
                        throw new IllegalArgumentException(
                                "memberRoles references unknown or default roleId: " + roleId);
                    }
                }
                copy.put(e.getKey(), assigned);
            }
            memberRoles = Map.copyOf(copy);
        }

        Set<UUID> statusIds = statuses.stream().map(TicketStatus::id).collect(Collectors.toSet());
        if (statusIds.size() != statuses.size()) {
            throw new IllegalArgumentException("statuses contains duplicate ids");
        }
        Set<UUID> typeIds = types.stream().map(TicketType::id).collect(Collectors.toSet());
        if (typeIds.size() != types.size()) {
            throw new IllegalArgumentException("types contains duplicate ids");
        }

        Set<UUID> ticketIds = new HashSet<>();
        for (Ticket t : tickets) {
            if (!ticketIds.add(t.id())) {
                throw new IllegalArgumentException("tickets contains duplicate id: " + t.id());
            }
            if (!statusIds.contains(t.statusId())) {
                throw new IllegalArgumentException(
                        "ticket " + t.id() + " references unknown statusId: " + t.statusId());
            }
            if (!typeIds.contains(t.typeId())) {
                throw new IllegalArgumentException(
                        "ticket " + t.id() + " references unknown typeId: " + t.typeId());
            }
        }
    }

    public static Project create(String id, String name, UUID owner) {
        return Defaults.newProject(id, name, owner);
    }

    public Optional<Role> findRole(UUID roleId) {
        for (Role r : roles) if (r.id().equals(roleId)) return Optional.of(r);
        return Optional.empty();
    }

    public Optional<TicketStatus> findStatus(UUID statusId) {
        for (TicketStatus s : statuses) if (s.id().equals(statusId)) return Optional.of(s);
        return Optional.empty();
    }

    public Optional<TicketType> findType(UUID typeId) {
        for (TicketType t : types) if (t.id().equals(typeId)) return Optional.of(t);
        return Optional.empty();
    }

    public Optional<Ticket> findTicket(UUID ticketId) {
        for (Ticket t : tickets) if (t.id().equals(ticketId)) return Optional.of(t);
        return Optional.empty();
    }

    public Role defaultRole() {
        return roles.get(roles.size() - 1);
    }

    public Project withName(String newName) {
        return new Project(id, newName, ownerUuid, defaultRoleId, allowNonMembers,
                roles, memberRoles, statuses, types, tickets);
    }

    public Project withOwner(UUID newOwner) {
        return new Project(id, name, newOwner, defaultRoleId, allowNonMembers,
                roles, memberRoles, statuses, types, tickets);
    }

    public Project withAllowNonMembers(boolean newAllow) {
        return new Project(id, name, ownerUuid, defaultRoleId, newAllow,
                roles, memberRoles, statuses, types, tickets);
    }

    public Project withRoles(List<Role> newRoles) {
        return new Project(id, name, ownerUuid, defaultRoleId, allowNonMembers,
                newRoles, memberRoles, statuses, types, tickets);
    }

    public Project withMemberRoles(Map<UUID, Set<UUID>> newMemberRoles) {
        return new Project(id, name, ownerUuid, defaultRoleId, allowNonMembers,
                roles, newMemberRoles, statuses, types, tickets);
    }

    public Project withStatuses(List<TicketStatus> newStatuses) {
        return new Project(id, name, ownerUuid, defaultRoleId, allowNonMembers,
                roles, memberRoles, newStatuses, types, tickets);
    }

    public Project withTypes(List<TicketType> newTypes) {
        return new Project(id, name, ownerUuid, defaultRoleId, allowNonMembers,
                roles, memberRoles, statuses, newTypes, tickets);
    }

    public Project withTickets(List<Ticket> newTickets) {
        return new Project(id, name, ownerUuid, defaultRoleId, allowNonMembers,
                roles, memberRoles, statuses, types, newTickets);
    }

    public Project addTicket(Ticket ticket) {
        List<Ticket> next = new ArrayList<>(tickets);
        next.add(ticket);
        return withTickets(next);
    }

    public Project replaceTicket(Ticket ticket) {
        List<Ticket> next = new ArrayList<>(tickets.size());
        boolean found = false;
        for (Ticket t : tickets) {
            if (t.id().equals(ticket.id())) {
                next.add(ticket);
                found = true;
            } else {
                next.add(t);
            }
        }
        if (!found) throw new IllegalArgumentException("no ticket with id " + ticket.id());
        return withTickets(next);
    }

    public Project removeTicket(UUID ticketId) {
        List<Ticket> next = new ArrayList<>(tickets.size());
        boolean removed = false;
        for (Ticket t : tickets) {
            if (t.id().equals(ticketId)) {
                removed = true;
            } else {
                next.add(t);
            }
        }
        if (!removed) throw new IllegalArgumentException("no ticket with id " + ticketId);
        return withTickets(next);
    }
}
