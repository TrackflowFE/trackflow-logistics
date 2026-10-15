package com.trackflow.shared.events;

public interface EventPublisher {

    void publish(DomainEvent event);
}
