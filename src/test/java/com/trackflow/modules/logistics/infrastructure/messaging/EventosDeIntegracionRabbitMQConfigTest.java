package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class EventosDeIntegracionRabbitMQConfigTest {

    private final Declarables declarables = new EventosDeIntegracionRabbitMQConfig().eventosDeIntegracion(
            "trackflow.events", "shipment.created", "trackflow.logistics.shipment-created",
            "trackflow.logistics.events.dlx", "trackflow.logistics.events.dlq");

    @Test
    void declaraLosDosExchangesComoTopicDurables() {
        List<TopicExchange> exchanges = declarables.getDeclarablesByType(TopicExchange.class);

        assertThat(exchanges).extracting(TopicExchange::getName)
                .containsExactlyInAnyOrder("trackflow.events", "trackflow.logistics.events.dlx");
        assertThat(exchanges).allMatch(TopicExchange::isDurable);
    }

    @Test
    void laColaDeEnviosCreadosDescartaHaciaLaDlq() {
        Queue cola = declarables.getDeclarablesByType(Queue.class).stream()
                .filter(q -> q.getName().equals("trackflow.logistics.shipment-created"))
                .findFirst().orElseThrow();

        assertThat(cola.getArguments())
                .containsEntry("x-dead-letter-exchange", "trackflow.logistics.events.dlx")
                .containsEntry("x-dead-letter-routing-key", "shipment.created");
    }

    @Test
    void enlazaEnviosCreadosYTodoLoDescartado() {
        List<Binding> bindings = declarables.getDeclarablesByType(Binding.class);

        assertThat(bindings).extracting(Binding::getDestination, Binding::getExchange, Binding::getRoutingKey)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("trackflow.logistics.shipment-created",
                                "trackflow.events", "shipment.created"),
                        org.assertj.core.groups.Tuple.tuple("trackflow.logistics.events.dlq",
                                "trackflow.logistics.events.dlx", "#"));
    }
}
