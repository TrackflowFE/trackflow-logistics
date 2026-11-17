package com.trackflow.modules.logistics.domain;

public class RecogidaFueraDeCiudadDeOrigenException extends RuntimeException {
    public RecogidaFueraDeCiudadDeOrigenException(Long ciudadId, Long origenId) {
        super("La ciudad de recogida " + ciudadId + " no coincide con el origen " + origenId);
    }
}
