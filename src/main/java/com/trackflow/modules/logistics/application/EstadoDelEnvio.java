package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.MotivoDeExcepcion;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Dónde va el envío ahora: estado, ubicación y movimientos admisibles.
 *
 * Se deriva del historial de eventos, no de columnas propias. El historial ya es la
 * fuente de la trazabilidad, así que derivar de él evita el único problema que tiene
 * guardar el estado aparte: que las dos versiones se desincronicen y ya no se sepa
 * cuál manda.
 *
 * @param registradoEn cuándo se registró el envío: ningún movimiento puede ser anterior
 * @param ultimoTipo último movimiento registrado, o null si el envío aún no se ha movido
 * @param centroActualId último centro por el que pasó, o null si todavía ninguno
 * @param ciudadActual dónde está el paquete; mientras no se haya movido, la de origen
 * @param ultimoMovimientoEn cuándo ocurrió el último movimiento, o null si no hay
 * @param rutaPlaneada ciudades por las que debe pasar desde la actual hasta el destino,
 *        en orden; la primera es la actual y la última el destino
 * @param siguienteParada la ciudad a la que debe llegar tras el próximo despacho
 * @param motivoDetenido por qué está detenido ahora mismo, o null si avanza con normalidad
 * @param notaDetenido lo que anotó el operador al reportar esa excepción
 * @param detenidoDesde cuándo ocurrió la excepción vigente
 */
public record EstadoDelEnvio(
        String trackingNumber,
        Instant registradoEn,
        EventType ultimoTipo,
        String estado,
        Instant ultimoMovimientoEn,
        Long centroActualId,
        String centroActualNombre,
        Ciudad ciudadActual,
        Ciudad origen,
        Ciudad destino,
        List<Ciudad> rutaPlaneada,
        Ciudad siguienteParada,
        Set<EventType> siguientesPermitidos,
        MotivoDeExcepcion motivoDetenido,
        String notaDetenido,
        Instant detenidoDesde) {

    /** Un envío sin excepción vigente: avanza con normalidad. */
    public EstadoDelEnvio(String trackingNumber, Instant registradoEn, EventType ultimoTipo, String estado,
            Instant ultimoMovimientoEn, Long centroActualId, String centroActualNombre, Ciudad ciudadActual,
            Ciudad origen, Ciudad destino, List<Ciudad> rutaPlaneada, Ciudad siguienteParada,
            Set<EventType> siguientesPermitidos) {
        this(trackingNumber, registradoEn, ultimoTipo, estado, ultimoMovimientoEn, centroActualId,
                centroActualNombre, ciudadActual, origen, destino, rutaPlaneada, siguienteParada,
                siguientesPermitidos, null, null, null);
    }

    /** Un envío está detenido mientras su último evento sea una excepción (HU-16). */
    public boolean detenido() {
        return motivoDetenido != null;
    }

    /** Traduce la regla abstracta a la ciudad concreta que exige. */
    public Ciudad ciudadQueExige(CiudadEsperada esperada) {
        return switch (esperada) {
            case ORIGEN -> origen;
            case DESTINO -> destino;
            case ACTUAL -> ciudadActual;
            case SIGUIENTE_PARADA -> siguienteParada;
        };
    }
}
