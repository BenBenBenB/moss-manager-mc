package com.mossman.domain.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ticket {
    private final long id;
    private final long projectId;
    private final int ticketNumber;
    private final String title;
    private final String description;
    private final String type;
    private final String status;
    private final Priority priority;
    private final List<UUID> assignees;
    private final List<UUID> observers;
    private final UUID creator;
    private final List<String> labels;
    private final long createdAt;
    private final long updatedAt;
    private final Long sprintId;

    public Ticket(long id, long projectId, int ticketNumber, String title, String description, String type, String status, Priority priority, List<UUID> assignees, List<UUID> observers, UUID creator, List<String> labels, long createdAt, long updatedAt, Long sprintId) {
        this.id = id;
        this.projectId = projectId;
        this.ticketNumber = ticketNumber;
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.priority = priority;
        this.assignees = new ArrayList<>(assignees);
        this.observers = new ArrayList<>(observers);
        this.creator = creator;
        this.labels = new ArrayList<>(labels);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sprintId = sprintId;
    }

    public long getId() { return id; }
    public long getProjectId() { return projectId; }
    public int getTicketNumber() { return ticketNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public List<UUID> getAssignees() { return List.copyOf(assignees); }
    public List<UUID> getObservers() { return List.copyOf(observers); }
    public UUID getCreator() { return creator; }
    public List<String> getLabels() { return List.copyOf(labels); }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public Long getSprintId() { return sprintId; }

    public String getUserFriendlyKey(String prefix) {
        return prefix + "-" + ticketNumber;
    }
}
