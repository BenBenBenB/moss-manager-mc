package com.mossman.domain.entities;

import java.util.UUID;

public record MailMessage(
        long id,
        UUID recipientId,
        UUID senderId,      // null = system message
        String senderName,
        String subject,
        String body,
        boolean isRead,
        long sentAt
) {}
