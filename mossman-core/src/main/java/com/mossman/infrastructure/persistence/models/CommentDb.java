package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.Comment;

import java.util.UUID;

@DatabaseTable(tableName = "comments")
public class CommentDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false)
    private long ticketId;

    @DatabaseField(canBeNull = false)
    private String authorId;

    @DatabaseField(canBeNull = false)
    private String authorName;

    @DatabaseField(canBeNull = false)
    private String message;

    @DatabaseField(canBeNull = false)
    private long createdAt;

    public CommentDb() {}

    public CommentDb(Comment comment) {
        this.id = comment.id();
        this.ticketId = comment.ticketId();
        this.authorId = comment.authorId().toString();
        this.authorName = comment.authorName();
        this.message = comment.message();
        this.createdAt = comment.createdAt();
    }

    public Comment toDomain() {
        return new Comment(id, ticketId, UUID.fromString(authorId), authorName, message, createdAt);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTicketId() { return ticketId; }
    public void setTicketId(long ticketId) { this.ticketId = ticketId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
