package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Reconstruye el estado actual de un envío a partir de su historial.
 *
 * Lo usan las dos puntas del mismo problema: quien admite un movimiento, para
 * validarlo contra el recorrido, y quien arma la lista de acciones disponibles, para
 * ofrecer solo lo válido. Compartir el cálculo es lo que garantiza que el operador no
 * vea una opción que el backend vaya a rechazar.
 */
@Service
@RequiredArgsConstructor
public class ConsultarEstadoDelEnvio {

    private final TrackedShipmentRepository trackedShipments;
    private final LogisticsEventRepository logisticsEvents;
    private final CatalogoDeCentros centros;
    private final CatalogoDeCiudades ciudades;
    private final PlanificarRuta planificarRuta;

    public EstadoDelEnvio ejecutar(String trackingNumber) {
        TrackedShipment envio = trackedShipments.exigir(trackingNumber);

        return desdeHistorial(envio, logisticsEvents.findHistorial(trackingNumber));
    }

    /**
     * Variante para quien ya tiene el historial en la mano, que se evita releerlo.
     * El historial viene ordenado por fecha de ocurrencia, así que el último de la
     * lista es el último movimiento del recorrido.
     */
    public EstadoDelEnvio desdeHistorial(TrackedShipment envio, List<LogisticsEvent> historial) {
        Ciudad origen = ciudades.exigir(envio.getOriginCityId());
        Ciudad destino = ciudades.exigir(envio.getDestinationCityId());

        Optional<LogisticsEvent> ultimo = contandoDesdeElFinal(historial, 0);
        EventType ultimoTipo = ultimo.map(LogisticsEvent::getType).orElse(null);
        EventType penultimoTipo = contandoDesdeElFinal(historial, 1).map(LogisticsEvent::getType).orElse(null);

        // El centro del último movimiento dice dónde está el paquete. Los eventos
        // viejos, registrados cuando el punto era texto libre, no tienen centro: de
        // esos no se puede deducir la ciudad, así que se cae al origen del envío.
        Optional<Centro> centroActual = ultimo.map(LogisticsEvent::getCenterId).flatMap(centros::porId);

        Ciudad ciudadActual = centroActual
                .map(centro -> ciudades.exigir(centro.getCityId()))
                .orElse(origen);

        List<Ciudad> ruta = rutaHastaElDestino(ciudadActual, destino);
        // La ruta empieza en la ciudad actual; si ya es el destino, la siguiente parada es él mismo.
        Ciudad siguienteParada = ruta.get(Math.min(1, ruta.size() - 1));

        return new EstadoDelEnvio(
                envio.getTrackingNumber(),
                envio.getRegisteredAt(),
                ultimoTipo,
                FlujoLogistico.estadoTras(ultimoTipo, penultimoTipo),
                ultimo.map(LogisticsEvent::getOccurredAt).orElse(null),
                centroActual.map(Centro::getId).orElse(null),
                centroActual.map(Centro::getName).orElse(null),
                ciudadActual,
                origen,
                destino,
                ruta,
                siguienteParada,
                FlujoLogistico.siguientesPermitidos(ultimoTipo, siguienteParada.id().equals(destino.id())));
    }

    /** El movimiento {@code posicion} lugares antes del último (0 es el último), si existe. */
    private static Optional<LogisticsEvent> contandoDesdeElFinal(List<LogisticsEvent> historial, int posicion) {
        int indice = historial.size() - 1 - posicion;
        return indice < 0 ? Optional.empty() : Optional.of(historial.get(indice));
    }

    /**
     * La ruta más corta desde donde está el paquete. Si la red ya no conecta las dos
     * ciudades (por ejemplo, porque se cerró un tramo después de registrar el envío),
     * se asume el trayecto directo en vez de dejar el envío sin ningún movimiento
     * posible: el registro de envíos nuevos ya exige que exista ruta.
     */
    private List<Ciudad> rutaHastaElDestino(Ciudad actual, Ciudad destino) {
        return planificarRuta.entre(actual.id(), destino.id())
                .map(ruta -> ruta.ciudades().stream().map(ciudades::exigir).toList())
                .orElseGet(() -> actual.id().equals(destino.id()) ? List.of(destino) : List.of(actual, destino));
    }
}
