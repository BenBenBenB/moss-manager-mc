package com.mossman.api.event;

import com.mossman.core.model.Ticket;

import java.util.UUID;

public sealed interface TicketEvent {

    record Created(String projectId, Ticket ticket) implements TicketEvent {}

    record Updated(String projectId, Ticket before, Ticket after) implements TicketEvent {}

    record Deleted(String projectId, UUID ticketId) implements TicketEvent {}
}
