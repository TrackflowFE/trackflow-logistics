package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import java.util.Optional;

public interface TrackedShipmentRepository {

    TrackedShipment save(TrackedShipment trackedShipment);

    boolean exists(String trackingNumber);

    /** Trae el envío conocido con sus ciudades, para las reglas de coherencia del centro. */
    Optional<TrackedShipment> porTrackingNumber(String trackingNumber);

    /**
     * Igual que {@link #porTrackingNumber}, pero bloquea el envío hasta que termine la
     * transacción, para que dos movimientos del mismo envío no se validen a la vez.
     */
    Optional<TrackedShipment> porTrackingNumberParaActualizar(String trackingNumber);

    /** Trae el envío o falla con 404: es la forma en que los casos de uso lo exigen. */
    default TrackedShipment exigir(String trackingNumber) {
        return porTrackingNumber(trackingNumber).orElseThrow(() -> new UnknownShipmentException(trackingNumber));
    }
}
