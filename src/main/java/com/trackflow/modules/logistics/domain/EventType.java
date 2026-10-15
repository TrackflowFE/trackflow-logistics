package com.trackflow.modules.logistics.domain;

public enum EventType {
    RECEIVED_AT_CENTER("AT_DISTRIBUTION_CENTER", "Recibido en centro"),
    DISPATCHED("IN_TRANSIT", "Despachado"),
    ARRIVED_AT_DESTINATION_CENTER("AT_DESTINATION_CENTER", "Llegada al centro destino"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Salida a reparto"),
    DELIVERED("DELIVERED", "Entregado");

    private final String resultingStatus;
    private final String etiqueta;

    EventType(String resultingStatus, String etiqueta) {
        this.resultingStatus = resultingStatus;
        this.etiqueta = etiqueta;
    }

    public String resultingStatus() {
        return resultingStatus;
    }

    /** Cómo se le nombra el movimiento al operador. */
    public String etiqueta() {
        return etiqueta;
    }
}
