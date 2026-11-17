package com.trackflow.modules.logistics.infrastructure.messaging;

import com.trackflow.modules.logistics.application.RegistrarEventoLogistico;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.MovimientoAntesDelRegistroException;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.RecogidaFueraDeCiudadDeOrigenException;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.shared.events.PickupCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PickupCompletedListener {
    private final RegistrarEventoLogistico registrar;

    @RabbitListener(queues = "${trackflow.logistics.events.pickup-completed-queue}")
    public void recibir(PickupCompletedEvent event) {
        if (event == null || !StringUtils.hasText(event.pickupNumber())
                || !StringUtils.hasText(event.trackingNumber()) || event.cityId() == null
                || event.pickedUpAt() == null || event.occurredAt() == null) {
            throw new AmqpRejectAndDontRequeueException("Evento de recogida incompleto");
        }
        try {
            registrar.ejecutarRecogida(event);
        } catch (FechaDeMovimientoInvalidaException | MovimientoAntesDelRegistroException
                | MovimientoFueraDeOrdenException | RecogidaFueraDeCiudadDeOrigenException
                | TransicionInvalidaException e) {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
        // UnknownShipmentException se reintenta: shipment.created puede llegar después.
    }
}
