package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.trackflow.modules.logistics.application.RegistrarEventoLogistico;
import com.trackflow.modules.logistics.domain.RecogidaFueraDeCiudadDeOrigenException;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.events.PickupCompletedEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class PickupCompletedListenerTest {
    private final RegistrarEventoLogistico registrar = mock(RegistrarEventoLogistico.class);
    private final PickupCompletedListener listener = new PickupCompletedListener(registrar);
    private final Instant ahora = Instant.parse("2026-09-02T12:00:00Z");
    private final PickupCompletedEvent evento = new PickupCompletedEvent("RC123456789012", "TF123", 1L,
            ahora, ahora);

    @Test
    void entregaLaRecogidaAlRegistrador() {
        listener.recibir(evento);
        verify(registrar).ejecutarRecogida(evento);
    }

    @Test
    void envioAunNoCreadoSeReintenta() {
        doThrow(new UnknownShipmentException("TF123")).when(registrar).ejecutarRecogida(evento);
        assertThatThrownBy(() -> listener.recibir(evento)).isInstanceOf(UnknownShipmentException.class);
    }

    @Test
    void errorDeDominioSeEnviaADlqSinReintentos() {
        doThrow(new RecogidaFueraDeCiudadDeOrigenException(2L, 1L)).when(registrar).ejecutarRecogida(evento);
        assertThatThrownBy(() -> listener.recibir(evento)).isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void mensajeIncompletoSeRechazaSinRegistrar() {
        PickupCompletedEvent incompleto = new PickupCompletedEvent("RC123", "TF123", null, ahora, ahora);
        assertThatThrownBy(() -> listener.recibir(incompleto)).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(registrar);
    }
}
