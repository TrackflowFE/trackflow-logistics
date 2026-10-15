package com.trackflow.shared.events;

import java.time.Instant;

/**
 * Evento de integración: lo publica logistics y lo consumen shipments y reports.
 * Vive en shared porque es el contrato entre módulos, no el modelo interno de ninguno.
 *
 * Distingue tres momentos, y confundirlos es lo que hacía retroceder el estado del
 * envío: {@code movedAt} es cuándo ocurrió el movimiento, {@code registeredAt} cuándo
 * se recibió el reporte y {@code occurredAt} cuándo se publicó este evento.
 *
 * {@code centerId} y {@code cityId} identifican el centro y la ciudad del catálogo donde
 * ocurrió el movimiento, para que quien agrupe por punto de la red o por ciudad no
 * dependa del nombre (que puede repetirse o cambiar); {@code cityName} es la etiqueta
 * para mostrar. Son null en los eventos históricos registrados con texto libre, antes
 * de que existiera el catálogo de centros.
 *
 * En un despacho, {@code nextStopCityId} y {@code nextStopCity} dicen hacia qué ciudad
 * salió el camión y {@code plannedLegHours} cuánto dura ese tramo según la red; en los
 * demás movimientos son null. Con ellos el reporte de retrasos sabe hacia dónde va un
 * envío en tránsito y cuánto debería tardar.
 */
public record EventoLogisticoRegistradoEvent(
        Long logisticsEventId,
        String trackingNumber,
        String eventType,
        String resultingStatus,
        String point,
        Long centerId,
        Long cityId,
        String cityName,
        Instant movedAt,
        Instant registeredAt,
        Instant occurredAt,
        Long nextStopCityId,
        String nextStopCity,
        Integer plannedLegHours) implements DomainEvent {

    /** Movimiento sin datos de despacho: la forma del contrato antes de que los llevara. */
    public EventoLogisticoRegistradoEvent(Long logisticsEventId, String trackingNumber, String eventType,
            String resultingStatus, String point, Long centerId, Long cityId, String cityName, Instant movedAt,
            Instant registeredAt, Instant occurredAt) {
        this(logisticsEventId, trackingNumber, eventType, resultingStatus, point, centerId, cityId, cityName,
                movedAt, registeredAt, occurredAt, null, null, null);
    }

    /**
     * Evento sin id de ciudad, como los registrados antes de que el evento lo llevara
     * (V17). Mantiene la forma anterior del contrato para quien todavía la construye.
     */
    public EventoLogisticoRegistradoEvent(Long logisticsEventId, String trackingNumber, String eventType,
            String resultingStatus, String point, Long centerId, String cityName, Instant movedAt,
            Instant registeredAt, Instant occurredAt) {
        this(logisticsEventId, trackingNumber, eventType, resultingStatus, point, centerId, null, cityName,
                movedAt, registeredAt, occurredAt);
    }
}
