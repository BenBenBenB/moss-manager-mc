package com.mossman.domain.events;

public interface DomainEventBus {
    void publish(DomainEvent event);
    <T extends DomainEvent> void subscribe(Class<T> eventType, java.util.function.Consumer<T> subscriber);
}
