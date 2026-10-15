package com.trackflow.shared.events;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();
}
