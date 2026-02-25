package com.mossman.domain.repositories;

import com.mossman.domain.entities.MailMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailRepository {
    MailMessage save(MailMessage message);
    Optional<MailMessage> findById(long id);
    /** Returns messages for recipient, newest-first, paginated. */
    List<MailMessage> findByRecipientId(UUID recipientId, int offset, int limit);
    long countByRecipientId(UUID recipientId);
    long countUnread(UUID recipientId);
}
