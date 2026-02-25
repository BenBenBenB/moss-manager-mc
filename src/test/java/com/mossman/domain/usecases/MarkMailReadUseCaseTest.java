package com.mossman.domain.usecases;

import com.mossman.domain.entities.MailMessage;
import com.mossman.domain.repositories.MailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarkMailReadUseCaseTest {

    private MailRepository mailRepository;
    private MarkMailReadUseCase useCase;

    private UUID recipientId;
    private MailMessage unreadMessage;

    @BeforeEach
    void setUp() {
        mailRepository = mock(MailRepository.class);
        useCase = new MarkMailReadUseCase(mailRepository);

        recipientId = UUID.randomUUID();
        unreadMessage = new MailMessage(5L, recipientId, null, "MossMan", "subject", "body", false, 1000L);
    }

    @Test
    void testExecute_MarksAsRead() {
        when(mailRepository.findById(5L)).thenReturn(Optional.of(unreadMessage));
        when(mailRepository.save(any(MailMessage.class))).thenAnswer(i -> i.getArgument(0));

        MailMessage result = useCase.execute(5L, recipientId);

        assertTrue(result.isRead());
        verify(mailRepository).save(any(MailMessage.class));
    }

    @Test
    void testExecute_ThrowsIfNotRecipient() {
        when(mailRepository.findById(5L)).thenReturn(Optional.of(unreadMessage));
        UUID otherId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(5L, otherId));
        verify(mailRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfMailNotFound() {
        when(mailRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(5L, recipientId));
    }

    @Test
    void testExecute_IdempotentIfAlreadyRead() {
        MailMessage alreadyRead = new MailMessage(5L, recipientId, null, "MossMan", "subject", "body", true, 1000L);
        when(mailRepository.findById(5L)).thenReturn(Optional.of(alreadyRead));

        MailMessage result = useCase.execute(5L, recipientId);

        assertTrue(result.isRead());
        verify(mailRepository, never()).save(any());
    }
}
