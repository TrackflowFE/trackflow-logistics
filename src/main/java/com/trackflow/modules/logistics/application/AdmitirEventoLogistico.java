package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.application.ValidadorDeRecorrido.LugarValidado;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaEnUsoException;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaInvalidaException;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Admite un evento reportado por un punto de la cadena y lo encola.
 * No lo registra: de eso se encarga {@link RegistrarEventoLogistico} al consumirlo.
 *
 * Aplica las reglas del recorrido ({@link ValidadorDeRecorrido}) antes de encolar: un
 * mensaje que no tiene sentido se rechaza con su código HTTP en vez de descartarse en
 * silencio en la cola de reintentos. El registro las vuelve a aplicar al consumir.
 */
@Service
@RequiredArgsConstructor
public class AdmitirEventoLogistico {

    /**
     * {@code centroId} identifica el centro del catálogo donde ocurrió el movimiento;
     * de él salen el nombre del punto y la ciudad, y contra él se validan las reglas
     * del recorrido. {@code ocurridoEn} es opcional: si el punto de la cadena no lo
     * reporta, se asume que el movimiento acaba de ocurrir. {@code claveIdempotencia}
     * también es opcional: si el cliente la envía, un reintento con la misma clave no
     * registra el movimiento dos veces.
     */
    public record Command(String trackingNumber, EventType tipo, Long centroId, String observaciones,
            String repartidorNombre, Instant ocurridoEn, String claveIdempotencia) {
    }

    /** El eventId es una columna de texto; una clave más larga no tiene uso legítimo. */
    static final int LARGO_MAXIMO_CLAVE = 100;

    private final TrackedShipmentRepository trackedShipments;
    private final LogisticsEventRepository logisticsEvents;
    private final ConsultarEstadoDelEnvio estadoDelEnvio;
    private final ValidadorDeRecorrido validador;
    private final EventoLogisticoPublisher publisher;
    private final Clock clock;

    public EventoLogisticoEntrante ejecutar(Command command) {
        TrackedShipment envio = trackedShipments.exigir(command.trackingNumber());

        // Un reintento (por ejemplo, tras un timeout) con una clave que ya se registró
        // recibe el mismo movimiento en vez de crear otro. Se resuelve antes de validar
        // el recorrido: el envío ya avanzó con ese movimiento y validarlo de nuevo lo
        // rechazaría como transición inválida.
        String clave = normalizar(command.claveIdempotencia());
        Optional<LogisticsEvent> yaRegistrado = Optional.ofNullable(clave).flatMap(logisticsEvents::porEventId);
        if (yaRegistrado.isPresent()) {
            return comoEntrante(yaRegistrado.get(), command.trackingNumber());
        }

        Instant ahora = clock.instant();
        Instant ocurridoEn = Objects.requireNonNullElse(command.ocurridoEn(), ahora);

        if (ocurridoEn.isAfter(ahora)) {
            throw new FechaDeMovimientoInvalidaException(ocurridoEn, ahora);
        }

        List<LogisticsEvent> historial = logisticsEvents.findHistorial(command.trackingNumber());
        EstadoDelEnvio estado = estadoDelEnvio.desdeHistorial(envio, historial);
        LugarValidado lugar = validador.validar(estado, command.tipo(), command.centroId(), ocurridoEn);

        EventoLogisticoEntrante evento = new EventoLogisticoEntrante(
                Objects.requireNonNullElse(clave, UUID.randomUUID().toString()),
                command.trackingNumber(),
                command.tipo(),
                lugar.centro().getId(),
                lugar.ciudad().id(),
                lugar.centro().getName(),
                lugar.ciudad().etiqueta(),
                command.observaciones(),
                command.repartidorNombre(),
                ocurridoEn);

        publisher.publicar(evento);

        return evento;
    }

    private static String normalizar(String clave) {
        if (!StringUtils.hasText(clave)) {
            return null;
        }
        String recortada = clave.trim();
        if (recortada.length() > LARGO_MAXIMO_CLAVE) {
            throw new ClaveDeIdempotenciaInvalidaException(LARGO_MAXIMO_CLAVE);
        }
        return recortada;
    }

    private static EventoLogisticoEntrante comoEntrante(LogisticsEvent evento, String trackingNumber) {
        if (!evento.getTrackingNumber().equals(trackingNumber)) {
            throw new ClaveDeIdempotenciaEnUsoException(evento.getEventId(), trackingNumber);
        }
        return new EventoLogisticoEntrante(
                evento.getEventId(),
                evento.getTrackingNumber(),
                evento.getType(),
                evento.getCenterId(),
                evento.getCityId(),
                evento.getPoint(),
                evento.getCityName(),
                evento.getNotes(),
                evento.getDelivererName(),
                evento.getOccurredAt());
    }
}
