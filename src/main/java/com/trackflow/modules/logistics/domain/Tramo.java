package com.trackflow.modules.logistics.domain;

/**
 * Trayecto directo que la red opera entre dos ciudades, en un solo sentido, y cuánto
 * tarda. Es la unidad con que se planifica la ruta de un envío: dos ciudades sin un
 * tramo entre ellas solo se conectan pasando por otras.
 */
public record Tramo(Long desdeCiudadId, Long hastaCiudadId, int horas) {
}
