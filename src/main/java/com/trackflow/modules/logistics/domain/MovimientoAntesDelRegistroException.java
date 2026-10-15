package com.trackflow.modules.logistics.domain;

import java.time.Instant;

/**
 * El movimiento dice haber ocurrido antes de que el envío se registrara: un paquete no
 * puede recibirse en un centro antes de existir. Es un 422.
 */
public class MovimientoAntesDelRegistroException extends RuntimeException {

    public MovimientoAntesDelRegistroException(String trackingNumber, Instant ocurridoEn, Instant registradoEn) {
        super(("El movimiento del envío %s ocurrió el %s, antes de que el envío se registrara (%s). "
                + "Revise la fecha reportada").formatted(trackingNumber, ocurridoEn, registradoEn));
    }
}
