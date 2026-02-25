package com.mossman.domain.usecases;

import com.mossman.domain.entities.MailMessage;
import com.mossman.domain.repositories.MailRepository;

import java.util.UUID;

public class MarkMailReadUseCase {
    private final MailRepository mailRepository;

    public MarkMailReadUseCase(MailRepository mailRepository) {
        this.mailRepository = mailRepository;
    }

    public MailMessage execute(long mailId, UUID requesterId) {
        MailMessage message = mailRepository.findById(mailId)
                .orElseThrow(() -> new IllegalArgumentException("Mail message not found: id=" + mailId));

        if (!message.recipientId().equals(requesterId)) {
            throw new IllegalArgumentException("You are not the recipient of this message");
        }

        if (message.isRead()) {
            return message;
        }

        MailMessage updated = new MailMessage(
                message.id(),
                message.recipientId(),
                message.senderId(),
                message.senderName(),
                message.subject(),
                message.body(),
                true,
                message.sentAt()
        );

        return mailRepository.save(updated);
    }
}
