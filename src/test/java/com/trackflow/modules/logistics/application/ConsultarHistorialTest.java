package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarHistorialTest {

    @Mock
    private LogisticsEventRepository logisticsEvents;

    @Mock
    private TrackedShipmentRepository trackedShipments;

    @InjectMocks
    private ConsultarHistorial consulta;

    @Test
    void devuelveLosMovimientosDelEnvio() {
        List<LogisticsEvent> historial = List.of(evento(1L, TN, EventType.RECEIVED_AT_CENTER,
                centro(10L, "CD Medellín", MEDELLIN), MEDELLIN, REGISTRO.plusSeconds(60)));
        when(trackedShipments.exists(TN)).thenReturn(true);
        when(logisticsEvents.findHistorial(TN)).thenReturn(historial);

        assertThat(consulta.ejecutar(TN)).isEqualTo(historial);
    }

    @Test
    void unEnvioSinMovimientosDevuelveListaVacia() {
        when(trackedShipments.exists(TN)).thenReturn(true);
        when(logisticsEvents.findHistorial(TN)).thenReturn(List.of());

        assertThat(consulta.ejecutar(TN)).isEmpty();
    }

    @Test
    void unEnvioInexistenteNoEsLoMismoQueUnoSinMovimientos() {
        when(trackedShipments.exists("NO-EXISTE")).thenReturn(false);

        assertThatThrownBy(() -> consulta.ejecutar("NO-EXISTE"))
                .isInstanceOf(UnknownShipmentException.class)
                .hasMessage("No existe un envío con el número de seguimiento NO-EXISTE");
        verifyNoInteractions(logisticsEvents);
    }
}
