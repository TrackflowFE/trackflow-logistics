package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventPublisherTest {

    private static final Instant T = Instant.parse("2026-09-02T10:00:00Z");
    private static final EventoLogisticoRegistradoEvent EVENTO = new EventoLogisticoRegistradoEvent(1L, "TF-1",
            "DISPATCHED", "IN_TRANSIT", "CD Medellín", 10L, 1L, "MEDELLÍN - ANTIOQUIA", T, T, T);

    @Mock
    private RabbitTemplate rabbit;

    private RabbitMQEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMQEventPublisher(rabbit, "trackflow.events", "logistics.event.registered");
    }

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void fueraDeUnaTransaccionPublicaEnSeguida() {
        publisher.publish(EVENTO);

        verify(rabbit).convertAndSend("trackflow.events", "logistics.event.registered", EVENTO);
    }

    @Test
    void dentroDeUnaTransaccionEsperaAlCommit() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publish(EVENTO);

        verifyNoInteractions(rabbit);
        List<TransactionSynchronization> pendientes = TransactionSynchronizationManager.getSynchronizations();
        assertThat(pendientes).hasSize(1);

        pendientes.get(0).afterCommit();

        verify(rabbit).convertAndSend("trackflow.events", "logistics.event.registered", EVENTO);
    }

    @Test
    void noPublicaEventosQueNoSonDeEstaApi() {
        EnvioCreadoEvent ajeno = new EnvioCreadoEvent("TF-1", "REGISTERED", "a", 1L, "M", "b", "c", 2L, "B", T, T);

        assertThatThrownBy(() -> publisher.publish(ajeno))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EnvioCreadoEvent");
        verifyNoInteractions(rabbit);
    }

    @Test
    void elContratoAnteriorDejaEnNuloLosDatosDeDespacho() {
        EventoLogisticoRegistradoEvent sinCiudad = new EventoLogisticoRegistradoEvent(1L, "TF-1", "DISPATCHED",
                "IN_TRANSIT", "CD", 10L, "MEDELLÍN", T, T, T);

        assertThat(sinCiudad.cityId()).isNull();
        assertThat(sinCiudad.cityName()).isEqualTo("MEDELLÍN");
        assertThat(sinCiudad.nextStopCityId()).isNull();
        assertThat(sinCiudad.plannedLegHours()).isNull();
    }
}
