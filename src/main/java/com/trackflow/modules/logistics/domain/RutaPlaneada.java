package com.trackflow.modules.logistics.domain;

import java.util.List;

/**
 * Las ciudades por las que debe pasar un envío, en orden, desde donde está hasta su
 * destino. La primera es la ciudad actual y la última la de destino; si son la misma,
 * la ruta tiene una sola ciudad.
 */
public record RutaPlaneada(List<Long> ciudades, int horas) {

    public RutaPlaneada {
        if (ciudades == null || ciudades.isEmpty()) {
            throw new IllegalArgumentException("una ruta tiene al menos una ciudad");
        }
        ciudades = List.copyOf(ciudades);
    }

    /** Ruta de una sola ciudad: el envío ya está en su destino. */
    public static RutaPlaneada enDestino(Long ciudadId) {
        return new RutaPlaneada(List.of(ciudadId), 0);
    }

    /**
     * La próxima ciudad a la que debe llegar el envío después de despacharse. Si ya
     * está en el destino, es el destino mismo.
     */
    public Long siguienteParada() {
        return ciudades.size() == 1 ? ciudades.get(0) : ciudades.get(1);
    }

    public Long destino() {
        return ciudades.get(ciudades.size() - 1);
    }
}
