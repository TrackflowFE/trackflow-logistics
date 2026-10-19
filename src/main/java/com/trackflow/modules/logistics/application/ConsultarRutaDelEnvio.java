package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
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

    @Transactional(readOnly = true)
    public RutaDelEnvio ejecutar(String trackingNumber) {
        TrackedShipment envio = trackedShipments.exigir(trackingNumber);
        List<LogisticsEvent> historial = logisticsEvents.findHistorial(trackingNumber);
        EstadoDelEnvio estado = estadoDelEnvio.desdeHistorial(envio, historial);

        List<Ciudad> recorridas = ciudadesRecorridas(estado, historial);
        List<Ciudad> pendientes = estado.rutaPlaneada().subList(1, estado.rutaPlaneada().size());
        boolean entregado = estado.ultimoTipo() == EventType.DELIVERED;

        List<Parada> paradas = new ArrayList<>();
        int total = recorridas.size() + pendientes.size();
        for (int i = 0; i < recorridas.size(); i++) {
            paradas.add(new Parada(recorridas.get(i), tipo(i, total), EstadoParada.RECORRIDA));
        }
        // La última recorrida es donde está el paquete, salvo que ya se haya entregado.
        if (!entregado) {
            int actual = recorridas.size() - 1;
            paradas.set(actual, new Parada(recorridas.get(actual), tipo(actual, total), EstadoParada.ACTUAL));
        }
        for (int i = 0; i < pendientes.size(); i++) {
            paradas.add(new Parada(pendientes.get(i), tipo(recorridas.size() + i, total), EstadoParada.PENDIENTE));
        }

        Ciudad siguiente = pendientes.stream().findFirst().orElse(null);
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
        List<Long> ciudadesDeIngreso = historial.stream()
                .filter(evento -> INGRESOS.contains(evento.getType()))
                .map(LogisticsEvent::getCityId)
                .filter(Objects::nonNull)
                .toList();
        for (Long cityId : ciudadesDeIngreso) {
            if (!Objects.equals(ultimo(recorridas).id(), cityId)) {
                recorridas.add(ciudades.exigir(cityId));
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
