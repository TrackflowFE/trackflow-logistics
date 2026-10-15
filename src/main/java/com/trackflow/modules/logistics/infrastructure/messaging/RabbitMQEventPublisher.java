package com.trackflow.modules.logistics.infrastructure.messaging;

import com.trackflow.shared.events.DomainEvent;
import com.trackflow.shared.events.EventPublisher;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publica los eventos de integración en el exchange compartido. Dentro de una transacción
 * espera al commit: así nunca se anuncia un movimiento que al final no quedó guardado.
 */
@Component
class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbit;
    private final String exchange;
    private final String eventoLogisticoRegistrado;

    RabbitMQEventPublisher(RabbitTemplate rabbit,
            @Value("${trackflow.events.exchange}") String exchange,
            @Value("${trackflow.events.logistics-registered-key}") String eventoLogisticoRegistrado) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.eventoLogisticoRegistrado = eventoLogisticoRegistrado;
    }

    @Override
    public void publish(DomainEvent event) {
        String routingKey = routingKey(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbit.convertAndSend(exchange, routingKey, event);
                }
            });
            return;
        }
        rabbit.convertAndSend(exchange, routingKey, event);
    }

    private String routingKey(DomainEvent event) {
        if (event instanceof EventoLogisticoRegistradoEvent) {
            return eventoLogisticoRegistrado;
        }
        throw new IllegalArgumentException("Esta API no publica eventos de tipo " + event.getClass().getSimpleName());
    }
}
