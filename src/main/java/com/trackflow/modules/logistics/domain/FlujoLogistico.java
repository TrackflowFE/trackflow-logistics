package com.trackflow.modules.logistics.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * El recorrido de un envío como máquina de estados: qué movimiento puede seguir a
 * cuál, y en qué ciudad debe ocurrir cada uno.
 *
 * Hasta ahora no existía tal cosa — cualquier evento se aceptaba en cualquier orden,
 * y por eso un envío llegó a tener tres RECEIVED_AT_CENTER seguidos en ciudades
 * distintas.
 *
 * Las transiciones se definen sobre el último movimiento y no sobre el estado. Aunque
 * hoy cada etapa ya tiene su propio estado, el movimiento es el dato primario: el
 * estado se deriva de él, y apoyarse en el derivado obligaría a mantener las dos
 * versiones de acuerdo.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlujoLogistico {

    /**
     * Estado de un envío que aún no tiene movimientos. Se escribe como texto y no
     * como ShipmentStatus porque logistics no puede importar el módulo shipments;
     * es el mismo criterio que ya usa EventType.resultingStatus().
     */
    public static final String ESTADO_REGISTRADO = "REGISTERED";

    /** Lo único que puede pasarle a un envío recién registrado es entrar a un centro. */
    private static final Set<EventType> DESDE_REGISTRADO = EnumSet.of(EventType.RECEIVED_AT_CENTER);

    private static final Map<EventType, Set<EventType>> SIGUIENTES = new EnumMap<>(EventType.class);

    static {
        // De un centro solo se sale despachando.
        SIGUIENTES.put(EventType.RECEIVED_AT_CENTER, EnumSet.of(EventType.DISPATCHED));
        // En tránsito puede llegar al destino, o a un hub intermedio y seguir.
        SIGUIENTES.put(EventType.DISPATCHED,
                EnumSet.of(EventType.ARRIVED_AT_DESTINATION_CENTER, EventType.RECEIVED_AT_CENTER));
        // Ya en la ciudad de destino, lo que sigue es repartir.
        SIGUIENTES.put(EventType.ARRIVED_AT_DESTINATION_CENTER, EnumSet.of(EventType.OUT_FOR_DELIVERY));
        SIGUIENTES.put(EventType.OUT_FOR_DELIVERY, EnumSet.of(EventType.DELIVERED));
        // Entregado es terminal: el recorrido se acabó.
        SIGUIENTES.put(EventType.DELIVERED, EnumSet.noneOf(EventType.class));
    }

    /**
     * Movimientos admisibles a continuación. {@code ultimo} es null cuando el envío
     * está registrado y todavía no se ha movido.
     */
    public static Set<EventType> siguientesPermitidos(EventType ultimo) {
        return Collections.unmodifiableSet(Optional.ofNullable(ultimo).map(SIGUIENTES::get).orElse(DESDE_REGISTRADO));
    }

    /**
     * Movimientos admisibles teniendo en cuenta la ruta. Después de un despacho caben
     * dos movimientos, pero cuál depende de a dónde va el camión: si la siguiente
     * parada de la ruta es el destino, solo la llegada; si no, solo el ingreso a un
     * hub. Ofrecer los dos dejaría registrar una llegada saltándose ciudades de la ruta,
     * o un hub cuando el paquete ya va directo al destino.
     */
    public static Set<EventType> siguientesPermitidos(EventType ultimo, boolean siguienteParadaEsDestino) {
        Set<EventType> permitidos = siguientesPermitidos(ultimo);
        if (ultimo != EventType.DISPATCHED) {
            return permitidos;
        }
        EventType queToca = siguienteParadaEsDestino
                ? EventType.ARRIVED_AT_DESTINATION_CENTER
                : EventType.RECEIVED_AT_CENTER;
        return Collections.unmodifiableSet(EnumSet.of(queToca));
    }

    public static boolean permite(EventType ultimo, EventType siguiente) {
        return siguientesPermitidos(ultimo).contains(siguiente);
    }

    /**
     * Estado en que queda el envío después de {@code ultimo}, sabiendo cuál fue el
     * movimiento anterior a ese ({@code anterior}, null si {@code ultimo} fue el primero).
     */
    public static String estadoTras(EventType ultimo, EventType anterior) {
        return Optional.ofNullable(ultimo).map(tipo -> estadoResultante(tipo, anterior)).orElse(ESTADO_REGISTRADO);
    }

    /**
     * Estado que deja un movimiento de tipo {@code tipo} cuando el movimiento previo
     * del envío fue {@code anterior} (null si es el primero).
     *
     * El primer RECEIVED_AT_CENTER es la entrada al centro de origen. Uno posterior
     * es un transbordo en un hub: el paquete sigue en camino hacia su destino, así que
     * el envío se queda EN TRÁNSITO. Si dejara AT_DISTRIBUTION_CENTER, como antes,
     * el cliente vería que su paquete volvió al centro de origen, y el progreso
     * retrocedería una etapa.
     */
    public static String estadoResultante(EventType tipo, EventType anterior) {
        if (tipo == EventType.RECEIVED_AT_CENTER && anterior != null) {
            return EventType.DISPATCHED.resultingStatus();
        }
        return tipo.resultingStatus();
    }

    /**
     * Ciudad en que debe estar el centro del movimiento. Depende del tipo y de si el
     * envío ya se movió: el primer RECEIVED_AT_CENTER es la entrada al centro de
     * origen, mientras que uno posterior es un hub de paso, y debe ser la siguiente
     * ciudad de la ruta.
     */
    public static CiudadEsperada ciudadEsperada(EventType tipo, EventType ultimo) {
        return switch (tipo) {
            case RECEIVED_AT_CENTER -> ultimo == null ? CiudadEsperada.ORIGEN : CiudadEsperada.SIGUIENTE_PARADA;
            case DISPATCHED -> CiudadEsperada.ACTUAL;
            case ARRIVED_AT_DESTINATION_CENTER, OUT_FOR_DELIVERY, DELIVERED -> CiudadEsperada.DESTINO;
        };
    }
}
