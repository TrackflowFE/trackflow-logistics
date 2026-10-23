package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.application.RegistrarEventoLogistico;
import com.trackflow.modules.logistics.domain.CentroInvalidoException;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventoLogisticoListenerTest {

    private static final EventoLogisticoMensaje MENSAJE = new EventoLogisticoMensaje("evt-1", "TF-1", "DISPATCHED",
            10L, 1L, "CD Medellín", "MEDELLÍN - ANTIOQUIA", null, null, Instant.parse("2026-09-02T10:00:00Z"));

    @Mock
    private RegistrarEventoLogistico registrar;

    @InjectMocks
    private RabbitMQEventoLogisticoListener listener;

    @Test
    void entregaElMensajeAlCasoDeUsoDeRegistro() {
        when(registrar.ejecutar(any())).thenReturn(Optional.empty());

        listener.recibir(MENSAJE);

        verify(registrar).ejecutar(MENSAJE.toEntrante());
    }

    @Test
    void unEnvioDesconocidoVaALaColaDeDescartadosSinReintentar() {
        when(registrar.ejecutar(any())).thenThrow(new UnknownShipmentException("TF-1"));

        assertThatThrownBy(() -> listener.recibir(MENSAJE))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasCauseInstanceOf(UnknownShipmentException.class)
                .hasMessageContaining("TF-1");
    }

    @Test
    void unMovimientoFueraDelRecorridoTampocoSeReintenta() {
        when(registrar.ejecutar(any())).thenThrow(
                new TransicionInvalidaException("TF-1", "DELIVERED", EventType.DISPATCHED, Set.of()));

        assertThatThrownBy(() -> listener.recibir(MENSAJE))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasCauseInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void unCentroInvalidoTampocoSeReintenta() {
        when(registrar.ejecutar(any())).thenThrow(CentroInvalidoException.noExiste(10L));

        assertThatThrownBy(() -> listener.recibir(MENSAJE))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void unFalloPasajeroSePropagaParaQueElBrokerReintente() {
        when(registrar.ejecutar(any())).thenThrow(new IllegalStateException("base ocupada"));

        assertThatThrownBy(() -> listener.recibir(MENSAJE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("base ocupada");
    }
}
