package com.trackflow.shared.geografia;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de consulta del catálogo de municipios.
 */
public interface CatalogoDeCiudades {

    Optional<Ciudad> porId(Long id);

    /**
     * Busca por nombre o por departamento para alimentar un autocompletado: quien
     * registra el envío escribe "bogo" y escoge de la lista, en lugar de teclear el
     * nombre y equivocarse.
     */
    List<Ciudad> buscar(String texto, int limite);

    /** Resuelve la ciudad o falla: es la forma en que los casos de uso la exigen. */
    default Ciudad exigir(Long id) {
        return porId(id).orElseThrow(() -> new CiudadDesconocidaException(id));
    }
}
