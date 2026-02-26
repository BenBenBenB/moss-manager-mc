package com.mossman.domain.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Project {
    private final long id;
    private final String ticketPrefix;
    private final String name;
    private final String description;
    private final String iconTexture;
    private final List<Status> statuses;
    private final List<TicketType> ticketTypes;
    private final List<RelationshipType> relationshipTypes;
    private final List<Member> members;
    private final Permission externalUserPermission;
    private final String textColor;

    public Project(long id, String ticketPrefix, String name, String description, String iconTexture,
                   List<Status> statuses, List<TicketType> ticketTypes, List<RelationshipType> relationshipTypes,
                   List<Member> members, Permission externalUserPermission, String textColor) {
        this.id = id;
        this.ticketPrefix = ticketPrefix;
        this.name = name;
        this.description = description;
        this.iconTexture = iconTexture;
        this.statuses = new ArrayList<>(statuses);
        this.ticketTypes = new ArrayList<>(ticketTypes);
        this.relationshipTypes = new ArrayList<>(relationshipTypes);
        this.members = new ArrayList<>(members);
        this.externalUserPermission = externalUserPermission;
        this.textColor = textColor;
    }

    public long getId() { return id; }
    public String getTicketPrefix() { return ticketPrefix; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconTexture() { return iconTexture; }
    public List<Status> getStatuses() { return List.copyOf(statuses); }
    public List<TicketType> getTicketTypes() { return List.copyOf(ticketTypes); }
    public List<RelationshipType> getRelationshipTypes() { return List.copyOf(relationshipTypes); }
    public List<Member> getMembers() { return List.copyOf(members); }
    public Permission getExternalUserPermission() { return externalUserPermission; }
    public String getTextColor() { return textColor; }

    /** Returns the member holding OWNER permission, if one exists. */
    public Optional<Member> getOwner() {
        return members.stream()
                .filter(m -> m.permission() == Permission.OWNER)
                .findFirst();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long id;
        private String ticketPrefix;
        private String name;
        private String description;
        private String iconTexture;
        private List<Status> statuses = new ArrayList<>();
        private List<TicketType> ticketTypes = new ArrayList<>();
        private List<RelationshipType> relationshipTypes = new ArrayList<>();
        private List<Member> members = new ArrayList<>();
        private Permission externalUserPermission = Permission.FORBID;
        private String textColor;

        public Builder id(long id) { this.id = id; return this; }
        public Builder ticketPrefix(String ticketPrefix) { this.ticketPrefix = ticketPrefix; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder iconTexture(String iconTexture) { this.iconTexture = iconTexture; return this; }
        public Builder statuses(List<Status> statuses) { this.statuses = statuses; return this; }
        public Builder ticketTypes(List<TicketType> ticketTypes) { this.ticketTypes = ticketTypes; return this; }
        public Builder relationshipTypes(List<RelationshipType> relationshipTypes) { this.relationshipTypes = relationshipTypes; return this; }
        public Builder members(List<Member> members) { this.members = members; return this; }
        public Builder externalUserPermission(Permission externalUserPermission) { this.externalUserPermission = externalUserPermission; return this; }
        public Builder textColor(String textColor) { this.textColor = textColor; return this; }

        public Project build() {
            return new Project(id, ticketPrefix, name, description, iconTexture, statuses, ticketTypes, relationshipTypes, members, externalUserPermission, textColor);
        }
    }
}
