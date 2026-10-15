package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.EstadoParada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.Parada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.RutaDelEnvio;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.TipoParada;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;

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

    public record CiudadResponse(Long id, String name) {

        static CiudadResponse from(Ciudad ciudad) {
            return ciudad == null ? null : new CiudadResponse(ciudad.id(), ciudad.etiqueta());
        }
    }

    /**
     * @param type ORIGIN, STOPOVER o DESTINATION
     * @param status VISITED, CURRENT o PENDING
     */
    public record ParadaResponse(Long cityId, String cityName, String type, String status) {

        static ParadaResponse from(Parada parada) {
            return new ParadaResponse(parada.ciudad().id(), parada.ciudad().etiqueta(), tipo(parada.tipo()),
                    estado(parada.estado()));
        }
    }

    public static RutaDelEnvioResponse from(RutaDelEnvio ruta) {
        return new RutaDelEnvioResponse(
                ruta.trackingNumber(),
                ruta.estado(),
                CiudadResponse.from(ruta.siguienteParada()),
                ruta.escalasPendientes(),
                ruta.paradas().stream().map(ParadaResponse::from).toList());
    }

    /** El enum del dominio va en español; el contrato de la API, en inglés. */
    private static String tipo(TipoParada tipo) {
        return switch (tipo) {
            case ORIGEN -> "ORIGIN";
            case ESCALA -> "STOPOVER";
            case DESTINO -> "DESTINATION";
        };
    }

    private static String estado(EstadoParada estado) {
        return switch (estado) {
            case RECORRIDA -> "VISITED";
            case ACTUAL -> "CURRENT";
            case PENDIENTE -> "PENDING";
        };
    }
}
