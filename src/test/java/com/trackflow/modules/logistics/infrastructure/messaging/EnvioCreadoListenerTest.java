package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.trackflow.modules.logistics.application.SeguirEnvioCreado;
import com.trackflow.shared.events.EnvioCreadoEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvioCreadoListenerTest {

    private static final Instant T = Instant.parse("2026-09-01T08:00:00Z");

    @Mock
    private SeguirEnvioCreado seguirEnvioCreado;

    @InjectMocks
    private EnvioCreadoListener listener;

    private static EnvioCreadoEvent evento(String tn, Instant registrado, Long origen, Long destino) {
        return new EnvioCreadoEvent(tn, "REGISTERED", "Remitente", origen, "MEDELLÍN", "Destinatario", "Calle 1",
                destino, "BOGOTÁ", registrado, T);
    }

    @Test
    void unEnvioCompletoSeEmpiezaASeguir() {
        EnvioCreadoEvent event = evento("TF-1", T, 1L, 2L);

        listener.recibir(event);

        verify(seguirEnvioCreado).ejecutar(event);
    }

    @Test
    void sinNumeroDeGuiaSeRechaza() {
        assertThatThrownBy(() -> listener.recibir(evento(" ", T, 1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trackingNumber");
        assertThatThrownBy(() -> listener.recibir(evento(null, T, 1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(seguirEnvioCreado);
    }

    @Test
    void sinFechaOCiudadesSeRechaza() {
        assertThatThrownBy(() -> listener.recibir(evento("TF-1", null, 1L, 2L)))
                .isInstanceOf(NullPointerException.class).hasMessage("registeredAt");
        assertThatThrownBy(() -> listener.recibir(evento("TF-1", T, null, 2L)))
                .isInstanceOf(NullPointerException.class).hasMessage("originCityId");
        assertThatThrownBy(() -> listener.recibir(evento("TF-1", T, 1L, null)))
                .isInstanceOf(NullPointerException.class).hasMessage("destinationCityId");
        verifyNoInteractions(seguirEnvioCreado);
    }
}
