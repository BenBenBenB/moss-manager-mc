package com.mossman.infrastructure.events;

import com.mossman.domain.events.DomainEvent;
import com.mossman.domain.events.DomainEventBus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SimpleEventBus implements DomainEventBus {
    private final Map<Class<? extends DomainEvent>, List<Consumer<?>>> subscribers = new HashMap<>();

    @Override
    public void publish(DomainEvent event) {
        List<Consumer<?>> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers != null) {
            for (Consumer<?> subscriber : eventSubscribers) {
                @SuppressWarnings("unchecked")
                Consumer<DomainEvent> consumer = (Consumer<DomainEvent>) subscriber;
                consumer.accept(event);
            }
        }
    }

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, Consumer<T> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(subscriber);
    }
}
