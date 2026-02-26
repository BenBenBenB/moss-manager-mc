package com.mossman.domain.usecases;

import com.mossman.domain.entities.MailMessage;
import com.mossman.domain.repositories.MailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SendMailUseCaseTest {

    private MailRepository mailRepository;
    private SendMailUseCase useCase;

    private UUID recipientId;

    @BeforeEach
    void setUp() {
        mailRepository = mock(MailRepository.class);
        useCase = new SendMailUseCase(mailRepository);
        recipientId = UUID.randomUUID();
    }

    private MailMessage buildMessage() {
        return new MailMessage(0, recipientId, null, "MossMan", "Hello", "World body", false, 0);
    }

    @Test
    void testExecute_Saves() {
        MailMessage msg = buildMessage();
        when(mailRepository.save(any(MailMessage.class))).thenAnswer(i -> {
            MailMessage m = i.getArgument(0);
            return new MailMessage(1L, m.recipientId(), m.senderId(), m.senderName(), m.subject(), m.body(), m.isRead(), m.sentAt());
        });

        MailMessage result = useCase.execute(msg);

        assertEquals(1L, result.id());
        assertEquals("Hello", result.subject());
        assertFalse(result.isRead());
        verify(mailRepository).save(any(MailMessage.class));
    }

    @Test
    void testExecute_ThrowsIfSubjectBlank() {
        MailMessage msg = new MailMessage(0, recipientId, null, "MossMan", "", "body", false, 0);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(msg));
        verify(mailRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfBodyBlank() {
        MailMessage msg = new MailMessage(0, recipientId, null, "MossMan", "subject", "", false, 0);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(msg));
        verify(mailRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfSubjectTooLong() {
        MailMessage msg = new MailMessage(0, recipientId, null, "MossMan", "S".repeat(129), "body", false, 0);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(msg));
        verify(mailRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfBodyTooLong() {
        MailMessage msg = new MailMessage(0, recipientId, null, "MossMan", "subject", "B".repeat(2049), false, 0);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(msg));
        verify(mailRepository, never()).save(any());
    }

    @Test
    void testExecute_NullSenderIdAllowed() {
        MailMessage msg = buildMessage();
        when(mailRepository.save(any(MailMessage.class))).thenAnswer(i -> i.getArgument(0));

        MailMessage result = useCase.execute(msg);

        assertNull(result.senderId());
        verify(mailRepository).save(any(MailMessage.class));
    }
}
