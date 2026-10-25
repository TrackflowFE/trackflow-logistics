package com.trackflow.modules.logistics.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Tipos de centro que puede tener un punto de la cadena logística.
 *
 * El enum se persiste en español; {@code enIngles} es como sale en el contrato de la API.
 * Va como argumento de cada constante para que un tipo nuevo no compile sin su traducción.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum TipoCentro {
    CENTRO_DISTRIBUCION("DISTRIBUTION_CENTER"),
    PUNTO_RECOLECCION("PICKUP_POINT"),
    OFICINA("OFFICE");

    private final String enIngles;
}
