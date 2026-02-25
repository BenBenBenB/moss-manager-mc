package com.mossman.domain.repositories;

import com.mossman.domain.entities.TimeLog;

import java.util.List;
import java.util.Optional;

public interface TimeLogRepository {
    TimeLog save(TimeLog log);
    Optional<TimeLog> findById(long id);
    List<TimeLog> findByTicketId(long ticketId, int offset, int limit);
    long countByTicketId(long ticketId);
    long sumMinutesByTicketId(long ticketId);
    void delete(long id);
}
