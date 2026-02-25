package com.mossman.domain.usecases;

import com.mossman.domain.entities.MailMessage;
import com.mossman.domain.repositories.MailRepository;

public class SendMailUseCase {
    private final MailRepository mailRepository;

    public SendMailUseCase(MailRepository mailRepository) {
        this.mailRepository = mailRepository;
    }

    public MailMessage execute(MailMessage message) {
        if (message.senderName() == null || message.senderName().isBlank()) {
            throw new IllegalArgumentException("Sender name must not be blank");
        }
        if (message.subject() == null || message.subject().isBlank()) {
            throw new IllegalArgumentException("Subject must not be blank");
        }
        if (message.subject().length() > 128) {
            throw new IllegalArgumentException("Subject must not exceed 128 characters");
        }
        if (message.body() == null || message.body().isBlank()) {
            throw new IllegalArgumentException("Body must not be blank");
        }
        if (message.body().length() > 2048) {
            throw new IllegalArgumentException("Body must not exceed 2048 characters");
        }

        MailMessage toSave = new MailMessage(
                message.id(),
                message.recipientId(),
                message.senderId(),
                message.senderName(),
                message.subject(),
                message.body(),
                false,
                System.currentTimeMillis()
        );

        return mailRepository.save(toSave);
    }
}
