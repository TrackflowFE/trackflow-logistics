package com.trackflow.shared.events;

import java.time.Instant;

/**
 * Evento de integración: lo publica shipments y lo consumen logistics y reports.
 * Vive en shared porque es el contrato entre módulos, no el modelo interno de ninguno.
 *
 * Lleva las ciudades de origen y destino por identificador y por etiqueta: el
 * identificador para agrupar, la etiqueta para mostrar sin volver a consultar el
 * catálogo.
 */
public record EnvioCreadoEvent(
        String trackingNumber,
        String status,
        String senderName,
        Long originCityId,
        String originCity,
        String recipientName,
        String recipientAddress,
        Long destinationCityId,
        String destinationCity,
        Instant registeredAt,
        Instant occurredAt) implements DomainEvent {
}
