package com.trackflow.modules.logistics.infrastructure.messaging;

import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import com.trackflow.modules.logistics.application.EventoLogisticoPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQEventoLogisticoPublisher implements EventoLogisticoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQEventoLogisticoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicar(EventoLogisticoEntrante evento) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                EventoLogisticoMensaje.from(evento));
    }
}
