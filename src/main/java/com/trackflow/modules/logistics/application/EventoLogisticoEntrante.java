package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.EventType;
import java.time.Instant;

/**
 * Evento logístico tal como lo reporta un punto de la cadena, antes de ser registrado.
 * Es el contrato de ingesta: lo produce el adaptador REST y lo consume el de mensajería.
 *
 * {@code punto} y {@code ciudadNombre} ya vienen resueltos desde la admisión (si se
 * reportó con {@code centroId}, son el nombre del centro y su ciudad; si se usó el
 * respaldo de texto libre, {@code punto} es ese texto y {@code ciudadNombre} es null,
 * porque de un texto libre no se puede saber la ciudad).
 */
public record EventoLogisticoEntrante(
        String eventId,
        String trackingNumber,
        EventType tipo,
        Long centroId,
        Long ciudadId,
        String punto,
        String ciudadNombre,
        String observaciones,
        String repartidorNombre,
        Instant ocurridoEn) {
}
