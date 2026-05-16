package com.mossman.api.spi;

import com.mossman.api.event.TicketEvent;

@FunctionalInterface
public interface TicketEventListener {

    void onTicketEvent(TicketEvent event);
}
