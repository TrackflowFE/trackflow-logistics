package com.trackflow.modules.logistics.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum EventType {
    RECEIVED_AT_CENTER("AT_DISTRIBUTION_CENTER", "Recibido en centro"),
    DISPATCHED("IN_TRANSIT", "Despachado"),
    ARRIVED_AT_DESTINATION_CENTER("AT_DESTINATION_CENTER", "Llegada al centro destino"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Salida a reparto"),
    DELIVERED("DELIVERED", "Entregado");

    private final String resultingStatus;
    /** Cómo se le nombra el movimiento al operador. */
    private final String etiqueta;
}
