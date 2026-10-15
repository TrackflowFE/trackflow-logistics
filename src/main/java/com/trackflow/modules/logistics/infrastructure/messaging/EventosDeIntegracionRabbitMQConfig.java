package com.trackflow.modules.logistics.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Eventos entre las tres APIs, en el exchange compartido trackflow.events:
 * esta API publica "evento logístico registrado" y consume "envío creado".
 *
 * La cola de consumo es propia de esta API (cada API que escucha un evento tiene la
 * suya), con cola de descartados para lo que no se pueda procesar tras los reintentos.
 */
@Configuration
public class EventosDeIntegracionRabbitMQConfig {

    @Bean
    Declarables eventosDeIntegracion(
            @Value("${trackflow.events.exchange}") String exchange,
            @Value("${trackflow.events.shipment-created-key}") String envioCreado,
            @Value("${trackflow.logistics.events.shipment-created-queue}") String cola,
            @Value("${trackflow.logistics.events.dead-letter-exchange}") String descartadosExchange,
            @Value("${trackflow.logistics.events.dead-letter-queue}") String descartados) {
        TopicExchange eventos = new TopicExchange(exchange, true, false);
        TopicExchange dlx = new TopicExchange(descartadosExchange, true, false);
        Queue envios = QueueBuilder.durable(cola)
                .deadLetterExchange(descartadosExchange).deadLetterRoutingKey(envioCreado).build();
        Queue dlq = QueueBuilder.durable(descartados).build();
        Binding enviosBinding = BindingBuilder.bind(envios).to(eventos).with(envioCreado);
        Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with("#");
        return new Declarables(eventos, dlx, envios, dlq, enviosBinding, dlqBinding);
    }
}
