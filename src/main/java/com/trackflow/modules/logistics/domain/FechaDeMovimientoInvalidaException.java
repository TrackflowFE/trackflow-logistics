package com.trackflow.modules.logistics.domain;

import java.time.Instant;

/**
 * La fecha en que se reporta que ocurrió el movimiento no es posible.
 */
public class FechaDeMovimientoInvalidaException extends RuntimeException {

    public FechaDeMovimientoInvalidaException(Instant ocurridoEn, Instant recibidoEn) {
        super(ocurridoEn == null
                ? "Falta la fecha en que ocurrió el movimiento"
                : "El movimiento no puede haber ocurrido en el futuro: se reportó %s y se recibió %s"
                        .formatted(ocurridoEn, recibidoEn));
    }
}
