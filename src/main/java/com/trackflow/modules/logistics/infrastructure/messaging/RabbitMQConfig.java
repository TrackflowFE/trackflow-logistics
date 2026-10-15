package com.trackflow.modules.logistics.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola interna de la API: las solicitudes de registro que admite el REST se procesan
 * aquí, de forma asíncrona. Los nombres son propios de esta API y distintos de los del
 * monolito (logistics.topic, logistics.events.queue), para que nunca compitan por los
 * mismos mensajes aunque compartan broker.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "logistics.api.topic";
    public static final String ROUTING_KEY = "logistics.event.requested";
    public static final String QUEUE = "logistics.api.requests.queue";
    public static final String DEAD_LETTER_EXCHANGE = "logistics.api.dlx";
    public static final String DEAD_LETTER_QUEUE = "logistics.api.requests.dlq";

    /**
     * Los mensajes viajan como JSON, para que cualquier productor pueda publicarlos. Se
     * confía en los paquetes del contrato entre APIs y de los mensajes internos; si un
     * mensaje llega sin cabecera de tipo, se usa el tipo del parámetro del listener.
     */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("com.trackflow.shared.events",
                "com.trackflow.modules.logistics.infrastructure.messaging");
    }

    @Bean
    TopicExchange logisticsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** Los mensajes que el consumidor rechaza terminan aquí en vez de perderse. */
    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue logisticsEventsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding logisticsEventsBinding() {
        return BindingBuilder.bind(logisticsEventsQueue()).to(logisticsExchange()).with(ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(ROUTING_KEY);
    }
}
