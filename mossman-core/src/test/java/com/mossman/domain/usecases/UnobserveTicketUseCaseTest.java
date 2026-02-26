package com.mossman.domain.usecases;

import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UnobserveTicketUseCaseTest {

    private TicketRepository ticketRepository;
    private UnobserveTicketUseCase useCase;

    private UUID requesterId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        useCase = new UnobserveTicketUseCase(ticketRepository);

        requesterId = UUID.randomUUID();

        ticket = new Ticket(10L, 1L, 1, "Test", "", "Task", "OPEN",
                Priority.MEDIUM, Collections.emptyList(), List.of(requesterId),
                requesterId, Collections.emptyList(), 0L, 0L, null);
    }

    @Test
    void testExecute_RemovesObserver() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        Ticket result = useCase.execute(10L, requesterId);

        assertFalse(result.getObservers().contains(requesterId));
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void testExecute_ThrowsIfNotObserving() {
        UUID nonObserver = UUID.randomUUID();
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, nonObserver));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testExecute_ThrowsIfTicketNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, requesterId));
    }
}
