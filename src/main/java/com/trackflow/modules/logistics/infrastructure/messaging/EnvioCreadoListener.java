package com.trackflow.modules.logistics.infrastructure.messaging;

import com.trackflow.modules.logistics.application.SeguirEnvioCreado;
import com.trackflow.shared.events.EnvioCreadoEvent;
import java.util.Objects;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Se entera de los envíos que registra la API de Envíos y acceso, para poder validar
 * después sus movimientos (origen, destino y fecha de registro). Recibir el mismo envío
 * dos veces no duplica nada: el envío seguido se identifica por su número de guía.
 */
@Component
public class EnvioCreadoListener {

    private final SeguirEnvioCreado seguirEnvioCreado;

    public EnvioCreadoListener(SeguirEnvioCreado seguirEnvioCreado) {
        this.seguirEnvioCreado = seguirEnvioCreado;
    }

    @RabbitListener(queues = "${trackflow.logistics.events.shipment-created-queue}")
    public void recibir(EnvioCreadoEvent event) {
        if (event.trackingNumber() == null || event.trackingNumber().isBlank()) {
            throw new IllegalArgumentException("Campo requerido: trackingNumber");
        }
        Objects.requireNonNull(event.registeredAt(), "registeredAt");
        Objects.requireNonNull(event.originCityId(), "originCityId");
        Objects.requireNonNull(event.destinationCityId(), "destinationCityId");
        seguirEnvioCreado.ejecutar(event);
    }
}
