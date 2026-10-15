package com.trackflow.modules.logistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Envíos que logistics conoce, alimentado por los eventos que publica shipments.
 * Evita que este módulo dependa del modelo de envíos para validar que existen — y,
 * desde este cambio, también para conocer sus ciudades de origen y destino, que las
 * reglas de coherencia del evento logístico necesitan sin poder leer el módulo
 * shipments.
 */
@Entity
@Table(name = "logistics_tracked_shipments")
public class TrackedShipment {

    @Id
    private String trackingNumber;

    @Column(nullable = false)
    private Instant registeredAt;

    @Column(nullable = false)
    private Long originCityId;

    @Column(nullable = false)
    private Long destinationCityId;

    protected TrackedShipment() {
    }

    public TrackedShipment(String trackingNumber, Instant registeredAt, Long originCityId,
            Long destinationCityId) {
        this.trackingNumber = trackingNumber;
        this.registeredAt = registeredAt;
        this.originCityId = originCityId;
        this.destinationCityId = destinationCityId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Long getOriginCityId() {
        return originCityId;
    }

    public Long getDestinationCityId() {
        return destinationCityId;
    }
}
