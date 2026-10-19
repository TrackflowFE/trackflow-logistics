package com.trackflow.modules.logistics.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * En qué ciudad debe estar el centro donde se registra un evento, según su tipo y el
 * punto del recorrido en que va el envío.
 *
 * Es lo que permite que el operador vea solo los centros que tienen sentido en vez
 * del catálogo entero: la misma regla que valida el registro alimenta la lista que se
 * le ofrece, así nunca se le muestra una opción que el backend vaya a rechazar.
 */
@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum CiudadEsperada {

    /** La ciudad de origen del envío: el primer recibido en centro ocurre ahí. */
    ORIGEN("la ciudad de origen del envío"),

    /** La ciudad de destino: llegada al centro destino, salida a reparto y entrega. */
    DESTINO("la ciudad de destino del envío"),

    /** Donde está el paquete ahora: se despacha desde el centro en el que está. */
    ACTUAL("la ciudad donde está el paquete"),

    /**
     * La siguiente ciudad de la ruta planeada desde donde está el paquete hasta su
     * destino: un hub intermedio. Antes valía cualquier ciudad menos la de destino, y
     * por eso un envío Medellín → Bogotá podía "pasar" por Cartagena.
     */
    SIGUIENTE_PARADA("la siguiente ciudad de la ruta del envío");

    private final String descripcion;
}
