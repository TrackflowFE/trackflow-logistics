package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Tramo;
import java.util.List;

/**
 * Puerto de consulta de la red de rutas.
 */
public interface RedDeRutas {

    /**
     * Tramos activos entre ciudades que tienen al menos un centro activo: un tramo que
     * llega a una ciudad sin centros no sirve, porque ahí no hay dónde recibir el envío.
     */
    List<Tramo> tramosActivos();
}
