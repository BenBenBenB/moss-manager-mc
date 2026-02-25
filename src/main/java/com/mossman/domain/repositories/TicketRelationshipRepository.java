package com.mossman.domain.repositories;

import com.mossman.domain.entities.TicketRelationship;

import java.util.List;
import java.util.Optional;

public interface TicketRelationshipRepository {
    TicketRelationship save(TicketRelationship relationship);
    Optional<TicketRelationship> findById(long id);
    /** Returns all relationships where ticketId is the source OR the target. */
    List<TicketRelationship> findByTicketId(long ticketId);
    Optional<TicketRelationship> findBySourceAndTargetAndType(long sourceId, long targetId, String type);
    void delete(long id);
}
