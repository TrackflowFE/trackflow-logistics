package com.trackflow.modules.logistics.api.dto;

import com.trackflow.shared.geografia.Ciudad;

/** Una ciudad tal como sale en las respuestas de la API: su id y su nombre para mostrar. */
public record CiudadResponse(Long id, String name) {

    static CiudadResponse from(Ciudad ciudad) {
        return new CiudadResponse(ciudad.id(), ciudad.etiqueta());
    }
}
