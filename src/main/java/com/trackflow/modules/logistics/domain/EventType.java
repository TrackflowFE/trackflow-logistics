package com.trackflow.modules.logistics.domain;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum EventType {
    RECEIVED_AT_CENTER("AT_DISTRIBUTION_CENTER", "Recibido en centro"),
    DISPATCHED("IN_TRANSIT", "Despachado"),
    ARRIVED_AT_DESTINATION_CENTER("AT_DESTINATION_CENTER", "Llegada al centro destino"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Salida a reparto"),
    DELIVERED("DELIVERED", "Entregado"),
    PICKED_UP("IN_CUSTODY", "Recogido a domicilio", false),
    /**
     * El envío no pudo seguir su curso (HU-16). No deja estado propio: conserva el del
     * último movimiento real, porque el paquete sigue donde estaba. Por eso su estado
     * es null y {@link FlujoLogistico} lo resuelve mirando hacia atrás.
     */
    EXCEPTION(null, "Excepción");

    private final String resultingStatus;
    /** Cómo se le nombra el movimiento al operador. */
    private final String etiqueta;
    private final boolean reportablePorOperador;

    EventType(String resultingStatus, String etiqueta) {
        this(resultingStatus, etiqueta, true);
    }

    EventType(String resultingStatus, String etiqueta, boolean reportablePorOperador) {
        this.resultingStatus = resultingStatus;
        this.etiqueta = etiqueta;
        this.reportablePorOperador = reportablePorOperador;
    }
}
