package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.TimeLog;

import java.util.UUID;

@DatabaseTable(tableName = "time_logs")
public class TimeLogDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false)
    private long ticketId;

    @DatabaseField(canBeNull = false)
    private String workerId;

    @DatabaseField(canBeNull = false)
    private String workerName;

    @DatabaseField(canBeNull = false)
    private long minutes;

    @DatabaseField
    private String note;

    @DatabaseField(canBeNull = false)
    private long loggedAt;

    public TimeLogDb() {}

    public TimeLogDb(TimeLog log) {
        this.id = log.id();
        this.ticketId = log.ticketId();
        this.workerId = log.workerId().toString();
        this.workerName = log.workerName();
        this.minutes = log.minutes();
        this.note = log.note();
        this.loggedAt = log.loggedAt();
    }

    public TimeLog toDomain() {
        return new TimeLog(id, ticketId, UUID.fromString(workerId), workerName, minutes,
                note != null ? note : "", loggedAt);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTicketId() { return ticketId; }
    public void setTicketId(long ticketId) { this.ticketId = ticketId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public long getMinutes() { return minutes; }
    public void setMinutes(long minutes) { this.minutes = minutes; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getLoggedAt() { return loggedAt; }
    public void setLoggedAt(long loggedAt) { this.loggedAt = loggedAt; }
}
