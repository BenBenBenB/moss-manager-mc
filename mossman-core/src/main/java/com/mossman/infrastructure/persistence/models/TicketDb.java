package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Ticket;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "tickets")
public class TicketDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false)
    private long projectId;

    @DatabaseField(canBeNull = false)
    private int ticketNumber;

    @DatabaseField(canBeNull = false)
    private String title;

    @DatabaseField
    private String description;

    @DatabaseField(canBeNull = false)
    private String type;

    @DatabaseField(canBeNull = false)
    private String status;

    @DatabaseField(canBeNull = false)
    private Priority priority;

    @DatabaseField(canBeNull = false)
    private UUID creator;

    @DatabaseField(canBeNull = false)
    private long createdAt;

    @DatabaseField(canBeNull = false)
    private long updatedAt;

    @DatabaseField
    private Long sprintId;

    /** Pipe-separated label strings; null or blank means no labels. */
    @DatabaseField
    private String labels;

    /** Comma-separated UUID strings; null or blank means no assignees. */
    @DatabaseField
    private String assignees;

    /** Comma-separated UUID strings; null or blank means no observers. */
    @DatabaseField
    private String observers;

    public TicketDb() {}

    public TicketDb(Ticket ticket) {
        this.id = ticket.getId();
        this.projectId = ticket.getProjectId();
        this.ticketNumber = ticket.getTicketNumber();
        this.title = ticket.getTitle();
        this.description = ticket.getDescription();
        this.type = ticket.getType();
        this.status = ticket.getStatus();
        this.priority = ticket.getPriority();
        this.creator = ticket.getCreator();
        this.createdAt = ticket.getCreatedAt();
        this.updatedAt = ticket.getUpdatedAt();
        this.sprintId = ticket.getSprintId();
        this.labels = ticket.getLabels().isEmpty() ? null :
                String.join("|", ticket.getLabels());
        this.assignees = ticket.getAssignees().isEmpty() ? null :
                ticket.getAssignees().stream().map(UUID::toString).collect(Collectors.joining(","));
        this.observers = ticket.getObservers().isEmpty() ? null :
                ticket.getObservers().stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    public Ticket toDomain() {
        List<String> parsedLabels = (labels == null || labels.isBlank())
                ? Collections.emptyList()
                : Arrays.stream(labels.split("\\|"))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList());
        List<UUID> parsedAssignees = (assignees == null || assignees.isBlank())
                ? Collections.emptyList()
                : Arrays.stream(assignees.split(","))
                        .filter(s -> !s.isBlank())
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
        List<UUID> parsedObservers = (observers == null || observers.isBlank())
                ? Collections.emptyList()
                : Arrays.stream(observers.split(","))
                        .filter(s -> !s.isBlank())
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
        return new Ticket(id, projectId, ticketNumber, title, description, type, status, priority,
                parsedAssignees, parsedObservers, creator, parsedLabels,
                createdAt, updatedAt, sprintId);
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProjectId() { return projectId; }
    public void setProjectId(long projectId) { this.projectId = projectId; }
    public int getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(int ticketNumber) { this.ticketNumber = ticketNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public UUID getCreator() { return creator; }
    public void setCreator(UUID creator) { this.creator = creator; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }
    public String getLabels() { return labels; }
    public void setLabels(String labels) { this.labels = labels; }
    public String getAssignees() { return assignees; }
    public void setAssignees(String assignees) { this.assignees = assignees; }
    public String getObservers() { return observers; }
    public void setObservers(String observers) { this.observers = observers; }
}
