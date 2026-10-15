package com.trackflow.modules.logistics.domain;

import java.time.Instant;

/**
 * El movimiento dice haber ocurrido antes que el último ya registrado. Se rechaza
 * porque el historial es lo que ordena el recorrido: si se pudiera intercalar un
 * evento en el pasado, la máquina de estados estaría validando contra un movimiento
 * que en realidad no es el anterior. Es un 422.
 *
 * Reportar tarde sigue siendo válido — lo que no se admite es reportar un movimiento
 * como anterior a otro que ya se registró.
 */
public class MovimientoFueraDeOrdenException extends RuntimeException {

    public MovimientoFueraDeOrdenException(String trackingNumber, Instant ocurridoEn, Instant ultimoMovimiento) {
        super(("El movimiento del envío %s dice haber ocurrido el %s, antes del último registrado (%s). "
                + "Un movimiento no puede intercalarse en el pasado del recorrido")
                        .formatted(trackingNumber, ocurridoEn, ultimoMovimiento));
    }
}
