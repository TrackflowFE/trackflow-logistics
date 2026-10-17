package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import com.trackflow.modules.logistics.domain.EventType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventoLogisticoPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQEventoLogisticoPublisher publisher;

    @Test
    void publicaEnLaColaInternaConElContratoDelMensaje() {
        EventoLogisticoEntrante evento = new EventoLogisticoEntrante("evt-1", "TF-1", EventType.RECEIVED_AT_CENTER,
                10L, 1L, "CD Medellín", "MEDELLÍN - ANTIOQUIA", null, null, Instant.parse("2026-09-02T10:00:00Z"));

        publisher.publicar(evento);

        ArgumentCaptor<Object> mensaje = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY),
                mensaje.capture());
        assertThat(mensaje.getValue()).isEqualTo(EventoLogisticoMensaje.from(evento));
    }
}
