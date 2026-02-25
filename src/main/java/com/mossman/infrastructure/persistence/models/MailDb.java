package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.MailMessage;

import java.util.UUID;

@DatabaseTable(tableName = "mail")
public class MailDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false)
    private String recipientId;

    /** Nullable — null means system message. Stored as UUID string. */
    @DatabaseField
    private String senderId;

    @DatabaseField(canBeNull = false)
    private String senderName;

    @DatabaseField(canBeNull = false)
    private String subject;

    @DatabaseField(canBeNull = false)
    private String body;

    @DatabaseField(canBeNull = false)
    private boolean isRead;

    @DatabaseField(canBeNull = false)
    private long sentAt;

    public MailDb() {}

    public MailDb(MailMessage message) {
        this.id = message.id();
        this.recipientId = message.recipientId().toString();
        this.senderId = message.senderId() != null ? message.senderId().toString() : null;
        this.senderName = message.senderName();
        this.subject = message.subject();
        this.body = message.body();
        this.isRead = message.isRead();
        this.sentAt = message.sentAt();
    }

    public MailMessage toDomain() {
        UUID senderUuid = (senderId != null && !senderId.isBlank()) ? UUID.fromString(senderId) : null;
        return new MailMessage(id, UUID.fromString(recipientId), senderUuid, senderName, subject, body, isRead, sentAt);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }
}
