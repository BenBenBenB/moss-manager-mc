package com.mossman.domain.repositories;

import com.mossman.domain.entities.Ticket;
import com.mossman.domain.query.TicketFilter;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(long id);
    List<Ticket> findByProjectId(long projectId, int offset, int limit);
    List<Ticket> findByProjectId(long projectId, TicketFilter filter, int offset, int limit);
    long countByProjectId(long projectId);
    long countByProjectId(long projectId, TicketFilter filter);
    void delete(long id);
    int getNextTicketNumber(long projectId);
}
