package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;
import java.util.Optional;
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
public class ConsultarEstadoDelEnvio {

    private final TrackedShipmentRepository trackedShipments;
    private final LogisticsEventRepository logisticsEvents;
    private final CatalogoDeCentros centros;
    private final CatalogoDeCiudades ciudades;
    private final PlanificarRuta planificarRuta;

    public ConsultarEstadoDelEnvio(TrackedShipmentRepository trackedShipments,
            LogisticsEventRepository logisticsEvents, CatalogoDeCentros centros, CatalogoDeCiudades ciudades,
            PlanificarRuta planificarRuta) {
        this.trackedShipments = trackedShipments;
        this.logisticsEvents = logisticsEvents;
        this.centros = centros;
        this.ciudades = ciudades;
        this.planificarRuta = planificarRuta;
    }

    public EstadoDelEnvio ejecutar(String trackingNumber) {
        TrackedShipment envio = trackedShipments.porTrackingNumber(trackingNumber)
                .orElseThrow(() -> new UnknownShipmentException(trackingNumber));

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

        LogisticsEvent ultimo = historial.isEmpty() ? null : historial.get(historial.size() - 1);
        EventType ultimoTipo = ultimo == null ? null : ultimo.getType();
        EventType penultimoTipo = historial.size() < 2 ? null : historial.get(historial.size() - 2).getType();

        // El centro del último movimiento dice dónde está el paquete. Los eventos
        // viejos, registrados cuando el punto era texto libre, no tienen centro: de
        // esos no se puede deducir la ciudad, así que se cae al origen del envío.
        Optional<Centro> centroActual = ultimo == null || ultimo.getCenterId() == null
                ? Optional.empty()
                : centros.porId(ultimo.getCenterId());

        Ciudad ciudadActual = centroActual
                .map(centro -> ciudades.exigir(centro.getCityId()))
                .orElse(origen);

        List<Ciudad> ruta = rutaHastaElDestino(ciudadActual, destino);
        Ciudad siguienteParada = ruta.size() == 1 ? ruta.get(0) : ruta.get(1);

        return new EstadoDelEnvio(
                envio.getTrackingNumber(),
                envio.getRegisteredAt(),
                ultimoTipo,
                FlujoLogistico.estadoTras(ultimoTipo, penultimoTipo),
                ultimo == null ? null : ultimo.getOccurredAt(),
                centroActual.map(Centro::getId).orElse(null),
                centroActual.map(Centro::getName).orElse(null),
                ciudadActual,
                origen,
                destino,
                ruta,
                siguienteParada,
                FlujoLogistico.siguientesPermitidos(ultimoTipo, siguienteParada.id().equals(destino.id())));
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
