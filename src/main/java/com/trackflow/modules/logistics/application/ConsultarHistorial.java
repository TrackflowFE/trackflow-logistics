package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devuelve los movimientos de un envío.
 *
 * Comprueba primero que el envío exista: sin esa comprobación, un número inexistente
 * y uno sin movimientos devolverían lo mismo (una lista vacía), y son situaciones
 * distintas para quien consulta.
 */
@Service
public class ConsultarHistorial {

    private final LogisticsEventRepository logisticsEvents;
    private final TrackedShipmentRepository trackedShipments;

    public ConsultarHistorial(LogisticsEventRepository logisticsEvents,
            TrackedShipmentRepository trackedShipments) {
        this.logisticsEvents = logisticsEvents;
        this.trackedShipments = trackedShipments;
    }

    @Transactional(readOnly = true)
    public List<LogisticsEvent> ejecutar(String trackingNumber) {
        if (!trackedShipments.exists(trackingNumber)) {
            throw new UnknownShipmentException(trackingNumber);
        }

        return logisticsEvents.findHistorial(trackingNumber);
    }
}
