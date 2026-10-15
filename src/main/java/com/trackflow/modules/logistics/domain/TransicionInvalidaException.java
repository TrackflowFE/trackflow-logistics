package com.trackflow.modules.logistics.domain;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * El movimiento existe y el envío también, pero no en este punto del recorrido: no se
 * puede despachar algo que ya salió a reparto, ni recibir en centro algo ya entregado.
 * Es un 422.
 */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(String trackingNumber, String estadoActual, EventType intentado,
            Set<EventType> permitidos) {
        super(mensaje(trackingNumber, estadoActual, intentado, permitidos));
    }

    private static String mensaje(String trackingNumber, String estadoActual, EventType intentado,
            Set<EventType> permitidos) {
        if (permitidos.isEmpty()) {
            return ("El envío %s ya está en estado %s y no admite más movimientos; se intentó registrar %s")
                    .formatted(trackingNumber, estadoActual, intentado);
        }

        return ("El envío %s está en estado %s y no admite un movimiento %s. Ahora solo es válido: %s")
                .formatted(trackingNumber, estadoActual, intentado,
                        permitidos.stream().map(Enum::name).collect(Collectors.joining(", ")));
    }
}
