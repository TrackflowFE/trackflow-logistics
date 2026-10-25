package com.trackflow.modules.logistics.infrastructure.messaging;

import com.trackflow.modules.logistics.application.RegistrarEventoLogistico;
import com.trackflow.modules.logistics.domain.CentroFueraDeCiudadException;
import com.trackflow.modules.logistics.domain.CentroInvalidoException;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.MovimientoAntesDelRegistroException;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por mensajería: recibe lo que reportan los puntos de la cadena
 * y lo entrega al mismo caso de uso que usa el adaptador REST.
 */
@Component
@RequiredArgsConstructor
public class RabbitMQEventoLogisticoListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventoLogisticoListener.class);

    private final RegistrarEventoLogistico registrarEventoLogistico;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void recibir(EventoLogisticoMensaje mensaje) {
        try {
            registrarEventoLogistico.ejecutar(mensaje.toEntrante());
        } catch (UnknownShipmentException | TransicionInvalidaException | MovimientoFueraDeOrdenException
                | MovimientoAntesDelRegistroException | CentroFueraDeCiudadException | CentroInvalidoException | FechaDeMovimientoInvalidaException e) {
            // Reintentar no va a cambiar la respuesta: ni el envío va a aparecer, ni el
            // movimiento va a caber en el recorrido. A la cola de descartados, con su motivo.
            log.warn("Evento {} del envío {} descartado: {}", mensaje.eventId(), mensaje.trackingNumber(),
                    e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
