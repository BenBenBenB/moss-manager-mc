package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.TicketRelationship;

@DatabaseTable(tableName = "ticket_relationships")
public class TicketRelationshipDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false)
    private String type;

    @DatabaseField(canBeNull = false)
    private long sourceTicketId;

    @DatabaseField(canBeNull = false)
    private long targetTicketId;

    public TicketRelationshipDb() {}

    public TicketRelationshipDb(TicketRelationship relationship) {
        this.id = relationship.id();
        this.type = relationship.type();
        this.sourceTicketId = relationship.sourceTicketId();
        this.targetTicketId = relationship.targetTicketId();
    }

    public TicketRelationship toDomain() {
        return new TicketRelationship(id, type, sourceTicketId, targetTicketId);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getSourceTicketId() { return sourceTicketId; }
    public void setSourceTicketId(long sourceTicketId) { this.sourceTicketId = sourceTicketId; }
    public long getTargetTicketId() { return targetTicketId; }
    public void setTargetTicketId(long targetTicketId) { this.targetTicketId = targetTicketId; }
}
