package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.PlanificadorDeRuta;
import com.trackflow.modules.logistics.domain.RutaPlaneada;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Ruta más corta entre dos ciudades sobre la red vigente.
 *
 * La ruta no se guarda con el envío: se recalcula desde donde está el paquete cada vez
 * que se consulta. Así, si un tramo se desactiva, el siguiente movimiento ya toma la
 * nueva ruta sin migrar datos.
 */
@Service
public class PlanificarRuta {

    private final RedDeRutas red;

    public PlanificarRuta(RedDeRutas red) {
        this.red = red;
    }

    public Optional<RutaPlaneada> entre(Long desdeCiudadId, Long hastaCiudadId) {
        return PlanificadorDeRuta.planificar(red.tramosActivos(), desdeCiudadId, hastaCiudadId);
    }
}
