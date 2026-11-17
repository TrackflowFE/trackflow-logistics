package com.trackflow.shared.events;

import java.time.Instant;

public record PickupCompletedEvent(
        String pickupNumber,
        String trackingNumber,
        Long cityId,
        Instant pickedUpAt,
        Instant occurredAt) implements DomainEvent {
}
