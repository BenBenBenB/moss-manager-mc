package com.mossman.domain.entities;

public record TicketRelationship(long id, String type, long sourceTicketId, long targetTicketId) {}
