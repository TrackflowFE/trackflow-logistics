package com.trackflow.modules.logistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Envíos que logistics conoce, alimentado por los eventos que publica shipments.
 * Evita que este módulo dependa del modelo de envíos para validar que existen — y,
 * desde este cambio, también para conocer sus ciudades de origen y destino, que las
 * reglas de coherencia del evento logístico necesitan sin poder leer el módulo
 * shipments.
 */
@Entity
@Getter
@Table(name = "logistics_tracked_shipments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackedShipment {

    @Id
    private String trackingNumber;

    @Column(nullable = false)
    private Instant registeredAt;

    @Column(nullable = false)
    private Long originCityId;

    @Column(nullable = false)
    private Long destinationCityId;

    public TrackedShipment(String trackingNumber, Instant registeredAt, Long originCityId,
            Long destinationCityId) {
        this.trackingNumber = trackingNumber;
        this.registeredAt = registeredAt;
        this.originCityId = originCityId;
        this.destinationCityId = destinationCityId;
    }
}
