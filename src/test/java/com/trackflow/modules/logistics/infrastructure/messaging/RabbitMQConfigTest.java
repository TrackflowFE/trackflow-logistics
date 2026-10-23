package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void laColaInternaEsDurableYDescartaHaciaSuDlx() {
        Queue cola = config.logisticsEventsQueue();

        assertThat(cola.getName()).isEqualTo("logistics.api.requests.queue");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMQConfig.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitMQConfig.ROUTING_KEY);
    }

    @Test
    void losNombresNoCompitenConLosDelMonolito() {
        TopicExchange exchange = config.logisticsExchange();

        assertThat(exchange.getName()).isEqualTo("logistics.api.topic").isNotEqualTo("logistics.topic");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(config.deadLetterExchange().getName()).isEqualTo(RabbitMQConfig.DEAD_LETTER_EXCHANGE);
        assertThat(config.deadLetterQueue().getName()).isEqualTo(RabbitMQConfig.DEAD_LETTER_QUEUE);
    }

    @Test
    void losBindingsUnenCadaColaConSuExchange() {
        Binding principal = config.logisticsEventsBinding();
        Binding descartados = config.deadLetterBinding();

        assertThat(principal.getDestination()).isEqualTo(RabbitMQConfig.QUEUE);
        assertThat(principal.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(principal.getRoutingKey()).isEqualTo(RabbitMQConfig.ROUTING_KEY);
        assertThat(descartados.getDestination()).isEqualTo(RabbitMQConfig.DEAD_LETTER_QUEUE);
        assertThat(descartados.getExchange()).isEqualTo(RabbitMQConfig.DEAD_LETTER_EXCHANGE);
    }

    @Test
    void losMensajesViajanComoJson() {
        assertThat(config.jsonMessageConverter()).isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
