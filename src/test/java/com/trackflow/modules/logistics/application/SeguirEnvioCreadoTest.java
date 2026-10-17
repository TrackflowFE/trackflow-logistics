package com.trackflow.modules.logistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.shared.events.EnvioCreadoEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeguirEnvioCreadoTest {

    @Mock
    private TrackedShipmentRepository trackedShipments;

    @InjectMocks
    private SeguirEnvioCreado seguir;

    @Test
    void guardaElEnvioConSusCiudadesYFechaDeRegistro() {
        Instant registrado = Instant.parse("2026-09-01T08:00:00Z");
        EnvioCreadoEvent event = new EnvioCreadoEvent("TF-9", "REGISTERED", "Remitente", 1L, "MEDELLÍN",
                "Destinatario", "Calle 1", 2L, "BOGOTÁ", registrado, registrado.plusSeconds(1));

        seguir.ejecutar(event);

        ArgumentCaptor<TrackedShipment> guardado = ArgumentCaptor.forClass(TrackedShipment.class);
        verify(trackedShipments).save(guardado.capture());
        assertThat(guardado.getValue().getTrackingNumber()).isEqualTo("TF-9");
        assertThat(guardado.getValue().getRegisteredAt()).isEqualTo(registrado);
        assertThat(guardado.getValue().getOriginCityId()).isEqualTo(1L);
        assertThat(guardado.getValue().getDestinationCityId()).isEqualTo(2L);
    }
}
