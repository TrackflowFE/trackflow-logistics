package com.trackflow.modules.logistics.domain;

/**
 * Se reportó una excepción sin decir por qué (HU-16). Es un 422: la petición está
 * bien formada, pero una excepción sin motivo no explica nada ni al cliente ni a la
 * operación, que es justamente para lo que sirve registrarla.
 */
public class MotivoDeExcepcionRequeridoException extends RuntimeException {

    public MotivoDeExcepcionRequeridoException(String trackingNumber) {
        super("Indique el motivo de la excepción del envío %s: %s"
                .formatted(trackingNumber, motivosDisponibles()));
    }

    private static String motivosDisponibles() {
        StringBuilder sb = new StringBuilder();
        for (MotivoDeExcepcion motivo : MotivoDeExcepcion.values()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(motivo.name()).append(" (").append(motivo.etiqueta()).append(')');
        }
        return sb.toString();
    }
}
