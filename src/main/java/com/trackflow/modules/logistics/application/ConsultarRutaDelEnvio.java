package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El itinerario completo de un envío: por qué ciudades ya pasó y por cuáles le falta
 * pasar hasta su destino.
 *
 * Junta dos fuentes. Lo recorrido sale del historial (las ciudades donde se recibió en
 * un centro). Lo pendiente sale de la ruta planeada desde donde está el paquete, que es
 * la misma que usan las reglas del recorrido. Así lo que ve el cliente como "próxima
 * escala" es exactamente lo que el operador podrá registrar.
 */
@Service
public class ConsultarRutaDelEnvio {

    /** Qué papel cumple la ciudad en el viaje. */
    public enum TipoParada { ORIGEN, ESCALA, DESTINO }

    /** Si el paquete ya pasó por ahí, está ahí o todavía no llega. */
    public enum EstadoParada { RECORRIDA, ACTUAL, PENDIENTE }

    public record Parada(Ciudad ciudad, TipoParada tipo, EstadoParada estado) {
    }

    /**
     * @param siguienteParada a dónde llega el paquete en su próximo traslado; null si ya
     *        está en el destino
     */
    public record RutaDelEnvio(String trackingNumber, String estado, List<Parada> paradas, Ciudad siguienteParada,
            int escalasPendientes) {
    }

    /** Los movimientos que ponen al paquete en una ciudad nueva. */
    private static final Set<EventType> INGRESOS = Set.of(EventType.RECEIVED_AT_CENTER,
            EventType.ARRIVED_AT_DESTINATION_CENTER);

    private final TrackedShipmentRepository trackedShipments;
    private final LogisticsEventRepository logisticsEvents;
    private final ConsultarEstadoDelEnvio estadoDelEnvio;
    private final CatalogoDeCiudades ciudades;

    public ConsultarRutaDelEnvio(TrackedShipmentRepository trackedShipments, LogisticsEventRepository logisticsEvents,
            ConsultarEstadoDelEnvio estadoDelEnvio, CatalogoDeCiudades ciudades) {
        this.trackedShipments = trackedShipments;
        this.logisticsEvents = logisticsEvents;
        this.estadoDelEnvio = estadoDelEnvio;
        this.ciudades = ciudades;
    }

    @Transactional(readOnly = true)
    public RutaDelEnvio ejecutar(String trackingNumber) {
        TrackedShipment envio = trackedShipments.porTrackingNumber(trackingNumber)
                .orElseThrow(() -> new UnknownShipmentException(trackingNumber));
        List<LogisticsEvent> historial = logisticsEvents.findHistorial(trackingNumber);
        EstadoDelEnvio estado = estadoDelEnvio.desdeHistorial(envio, historial);

        List<Ciudad> recorridas = ciudadesRecorridas(estado, historial);
        List<Ciudad> pendientes = estado.rutaPlaneada().subList(1, estado.rutaPlaneada().size());
        boolean entregado = estado.ultimoTipo() == EventType.DELIVERED;

        List<Parada> paradas = new ArrayList<>();
        int total = recorridas.size() + pendientes.size();
        for (int i = 0; i < recorridas.size(); i++) {
            boolean esLaActual = i == recorridas.size() - 1 && !entregado;
            paradas.add(new Parada(recorridas.get(i), tipo(i, total),
                    esLaActual ? EstadoParada.ACTUAL : EstadoParada.RECORRIDA));
        }
        for (int i = 0; i < pendientes.size(); i++) {
            paradas.add(new Parada(pendientes.get(i), tipo(recorridas.size() + i, total), EstadoParada.PENDIENTE));
        }

        Ciudad siguiente = pendientes.isEmpty() ? null : pendientes.get(0);
        // La última pendiente es el destino; las demás son escalas.
        int escalasPendientes = Math.max(0, pendientes.size() - 1);
        return new RutaDelEnvio(trackingNumber, estado.estado(), List.copyOf(paradas), siguiente, escalasPendientes);
    }

    /**
     * El origen y luego cada ciudad nueva donde el paquete entró a un centro, en orden.
     * Si la ciudad actual no quedó en esa lista (eventos viejos sin ciudad), se agrega al
     * final para que el itinerario termine donde está el paquete.
     */
    private List<Ciudad> ciudadesRecorridas(EstadoDelEnvio estado, List<LogisticsEvent> historial) {
        List<Ciudad> recorridas = new ArrayList<>();
        recorridas.add(estado.origen());
        for (LogisticsEvent evento : historial) {
            if (INGRESOS.contains(evento.getType()) && evento.getCityId() != null
                    && !Objects.equals(ultimo(recorridas).id(), evento.getCityId())) {
                recorridas.add(ciudades.exigir(evento.getCityId()));
            }
        }
        if (!Objects.equals(ultimo(recorridas).id(), estado.ciudadActual().id())) {
            recorridas.add(estado.ciudadActual());
        }
        return recorridas;
    }

    private static Ciudad ultimo(List<Ciudad> lista) {
        return lista.get(lista.size() - 1);
    }

    private static TipoParada tipo(int posicion, int total) {
        if (posicion == 0) {
            return TipoParada.ORIGEN;
        }
        return posicion == total - 1 ? TipoParada.DESTINO : TipoParada.ESCALA;
    }
}
