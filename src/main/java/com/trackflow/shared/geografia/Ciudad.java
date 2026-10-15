package com.trackflow.shared.geografia;

/**
 * Municipio del catálogo.
 *
 * Vive en shared porque es dato de referencia que no pertenece a ningún módulo de
 * negocio: shipments lo usa para el origen y el destino, y reports para la etiqueta
 * que ve el cliente. Es el mismo criterio que puso los eventos de integración aquí.
 */
public record Ciudad(Long id, String nombre, String departamento) {

    /** Lo que se muestra al cliente: "MEDELLÍN - ANTIOQUIA". */
    public String etiqueta() {
        return nombre + " - " + departamento;
    }
}
