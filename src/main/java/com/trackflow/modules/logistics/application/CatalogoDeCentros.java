package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CentroInvalidoException;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de consulta del catálogo de centros.
 */
public interface CatalogoDeCentros {

    Optional<Centro> porId(Long id);

    /**
     * Busca por nombre y, opcionalmente, filtra por ciudad — solo activos. Alimenta
     * el autocompletado de centro al registrar un evento logístico.
     */
    List<Centro> buscar(String texto, Long ciudadId, int limite);

    /**
     * Resuelve el centro para admitir un evento, o falla: no existe, o existe pero
     * está inactivo.
     */
    default Centro exigir(Long id) {
        Centro centro = porId(id).orElseThrow(() -> CentroInvalidoException.noExiste(id));
        if (!centro.isActive()) {
            throw CentroInvalidoException.inactivo(id, centro.getName());
        }
        return centro;
    }
}
