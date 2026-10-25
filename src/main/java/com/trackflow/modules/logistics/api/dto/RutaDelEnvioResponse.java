package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.EstadoParada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.Parada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.RutaDelEnvio;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.TipoParada;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Itinerario del envío: las ciudades por las que pasó y las que le faltan, con las
 * escalas marcadas. Campos en inglés, igual que el resto del contrato.
 *
 * @param nextStop la ciudad a la que llega en su próximo traslado; null si ya está en
 *        el destino
 * @param pendingStopovers cuántas escalas le faltan antes del destino
 */
public record RutaDelEnvioResponse(
        String trackingNumber,
        String currentStatus,
        CiudadResponse nextStop,
        int pendingStopovers,
        List<ParadaResponse> stops) {

    /**
     * @param type ORIGIN, STOPOVER o DESTINATION
     * @param status VISITED, CURRENT o PENDING
     */
    public record ParadaResponse(Long cityId, String cityName, String type, String status) {

        static ParadaResponse from(Parada parada) {
            return new ParadaResponse(parada.ciudad().id(), parada.ciudad().etiqueta(),
                    TIPOS.get(parada.tipo()), ESTADOS.get(parada.estado()));
        }
    }

    /** El enum del dominio va en español; el contrato de la API, en inglés. */
    private static final Map<TipoParada, String> TIPOS = new EnumMap<>(Map.of(
            TipoParada.ORIGEN, "ORIGIN",
            TipoParada.ESCALA, "STOPOVER",
            TipoParada.DESTINO, "DESTINATION"));

    private static final Map<EstadoParada, String> ESTADOS = new EnumMap<>(Map.of(
            EstadoParada.RECORRIDA, "VISITED",
            EstadoParada.ACTUAL, "CURRENT",
            EstadoParada.PENDIENTE, "PENDING"));

    public static RutaDelEnvioResponse from(RutaDelEnvio ruta) {
        return new RutaDelEnvioResponse(
                ruta.trackingNumber(),
                ruta.estado(),
                Optional.ofNullable(ruta.siguienteParada()).map(CiudadResponse::from).orElse(null),
                ruta.escalasPendientes(),
                ruta.paradas().stream().map(ParadaResponse::from).toList());
    }
}
