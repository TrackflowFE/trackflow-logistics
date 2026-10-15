package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import java.time.Instant;

/** Un movimiento del historial. Campos en inglés, igual que TrackingResponse (bug #89). */
public record EventoLogisticoResponse(
        Long id,
        String trackingNumber,
        String type,
        String resultingStatus,
        Long centerId,
        String point,
        Long cityId,
        String cityName,
        String notes,
        String delivererName,
        /** Solo en DISPATCHED: la ciudad hacia la que salió el camión (un hub o el destino). */
        Long nextStopCityId,
        String nextStopCity,
        Instant occurredAt,
        Instant registeredAt) {

    /** @param anterior tipo del movimiento previo del mismo envío, o null si este es el primero */
    public static EventoLogisticoResponse from(LogisticsEvent event, EventType anterior) {
        return new EventoLogisticoResponse(
                event.getId(),
                event.getTrackingNumber(),
                event.getType().name(),
                FlujoLogistico.estadoResultante(event.getType(), anterior),
                event.getCenterId(),
                event.getPoint(),
                event.getCityId(),
                event.getCityName(),
                event.getNotes(),
                event.getDelivererName(),
                event.getNextStopCityId(),
                event.getNextStopCity(),
                event.getOccurredAt(),
                event.getRegisteredAt());
    }
}
