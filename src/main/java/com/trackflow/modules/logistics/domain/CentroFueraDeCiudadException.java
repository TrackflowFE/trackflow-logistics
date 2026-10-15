package com.trackflow.modules.logistics.domain;

/**
 * El centro indicado no está en la ciudad que el recorrido exige para ese movimiento
 * — la de origen, la de destino, aquella donde el paquete está ahora o la siguiente de
 * su ruta. Es un 422.
 */
public class CentroFueraDeCiudadException extends RuntimeException {

    private CentroFueraDeCiudadException(String mensaje) {
        super(mensaje);
    }

    /** @param ciudadEsperadaEtiqueta la ciudad concreta que se esperaba */
    public static CentroFueraDeCiudadException para(EventType tipo, CiudadEsperada esperada, String nombreCentro,
            String ciudadCentro, String ciudadEsperadaEtiqueta) {
        return new CentroFueraDeCiudadException(
                ("El centro '%s' está en %s, pero un movimiento %s debe registrarse en un centro de %s (%s)")
                        .formatted(nombreCentro, ciudadCentro, tipo, ciudadEsperadaEtiqueta, esperada.descripcion()));
    }
}
