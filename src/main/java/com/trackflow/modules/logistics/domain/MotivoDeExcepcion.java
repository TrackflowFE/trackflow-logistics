package com.trackflow.modules.logistics.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Por qué un envío no pudo seguir su curso (HU-16).
 *
 * Es una lista cerrada y no texto libre: el cliente ve estos motivos en su rastreo, y
 * la operación necesita contarlos para saber qué falla más. Lo particular de cada caso
 * —el apartamento equivocado, quién recibió— va en la nota, que sí es libre.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum MotivoDeExcepcion {

    RECIPIENT_ABSENT("Destinatario ausente"),
    WRONG_ADDRESS("Dirección incorrecta"),
    DAMAGED("Paquete dañado"),
    REFUSED("Rechazado por el destinatario"),
    HELD("Retenido");

    /** Cómo se le nombra al cliente y al operador. */
    private final String etiqueta;
}
