package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.envio;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.application.ValidadorDeRecorrido.LugarValidado;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaEnUsoException;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaInvalidaException;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdmitirEventoLogisticoTest {

    private static final Instant AHORA = Instant.parse("2026-09-02T12:00:00Z");

    // exigir() es un método por defecto del repositorio: el mock debe ejecutarlo de verdad.
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TrackedShipmentRepository trackedShipments;

    @Mock
    private LogisticsEventRepository logisticsEvents;

    @Mock
    private ConsultarEstadoDelEnvio estadoDelEnvio;

    @Mock
    private ValidadorDeRecorrido validador;

    @Mock
    private EventoLogisticoPublisher publisher;

    private AdmitirEventoLogistico admitir;

    private final TrackedShipment envio = envio(MEDELLIN, BOGOTA);
    private final Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);
    private final EstadoDelEnvio estado = mock(EstadoDelEnvio.class);

    @BeforeEach
    void setUp() {
        admitir = new AdmitirEventoLogistico(trackedShipments, logisticsEvents, estadoDelEnvio, validador, publisher,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static AdmitirEventoLogistico.Command comando(Instant ocurridoEn, String clave) {
        return new AdmitirEventoLogistico.Command(TN, EventType.RECEIVED_AT_CENTER, 10L, "frágil", null, ocurridoEn,
                clave);
    }

    private void envioValido() {
        when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.of(envio));
        when(logisticsEvents.findHistorial(TN)).thenReturn(List.of());
        when(estadoDelEnvio.desdeHistorial(envio, List.of())).thenReturn(estado);
    }

    @Test
    void admiteElMovimientoYLoPublicaConLosDatosDelCentro() {
        envioValido();
        Instant ocurrido = AHORA.minusSeconds(600);
        when(validador.validar(estado, EventType.RECEIVED_AT_CENTER, 10L, ocurrido))
                .thenReturn(new LugarValidado(cdMedellin, MEDELLIN));

        EventoLogisticoEntrante evento = admitir.ejecutar(comando(ocurrido, null));

        assertThat(evento.eventId()).isNotBlank();
        assertThat(evento.trackingNumber()).isEqualTo(TN);
        assertThat(evento.tipo()).isEqualTo(EventType.RECEIVED_AT_CENTER);
        assertThat(evento.centroId()).isEqualTo(10L);
        assertThat(evento.ciudadId()).isEqualTo(MEDELLIN.id());
        assertThat(evento.punto()).isEqualTo("CD Medellín");
        assertThat(evento.ciudadNombre()).isEqualTo(MEDELLIN.etiqueta());
        assertThat(evento.observaciones()).isEqualTo("frágil");
        assertThat(evento.ocurridoEn()).isEqualTo(ocurrido);
        verify(publisher).publicar(evento);
    }

    @Test
    void sinFechaSeAsumeQueAcabaDeOcurrir() {
        envioValido();
        when(validador.validar(estado, EventType.RECEIVED_AT_CENTER, 10L, AHORA))
                .thenReturn(new LugarValidado(cdMedellin, MEDELLIN));

        EventoLogisticoEntrante evento = admitir.ejecutar(comando(null, null));

        assertThat(evento.ocurridoEn()).isEqualTo(AHORA);
    }

    @Test
    void conClaveNuevaLaUsaComoIdentificadorDelEvento() {
        envioValido();
        when(logisticsEvents.porEventId("clave-1")).thenReturn(Optional.empty());
        when(validador.validar(any(), any(), any(), any())).thenReturn(new LugarValidado(cdMedellin, MEDELLIN));

        EventoLogisticoEntrante evento = admitir.ejecutar(comando(AHORA, "  clave-1  "));

        assertThat(evento.eventId()).isEqualTo("clave-1");
        ArgumentCaptor<EventoLogisticoEntrante> publicado = ArgumentCaptor.forClass(EventoLogisticoEntrante.class);
        verify(publisher).publicar(publicado.capture());
        assertThat(publicado.getValue().eventId()).isEqualTo("clave-1");
    }

    @Test
    void unReintentoConLaMismaClaveDevuelveElMovimientoYaRegistradoSinPublicar() {
        when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.of(envio));
        LogisticsEvent registrado = evento(7L, TN, EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN,
                REGISTRO.plusSeconds(60));
        when(logisticsEvents.porEventId("evt-7")).thenReturn(Optional.of(registrado));

        EventoLogisticoEntrante evento = admitir.ejecutar(comando(null, "evt-7"));

        assertThat(evento.eventId()).isEqualTo("evt-7");
        assertThat(evento.centroId()).isEqualTo(10L);
        assertThat(evento.ciudadId()).isEqualTo(MEDELLIN.id());
        assertThat(evento.ocurridoEn()).isEqualTo(REGISTRO.plusSeconds(60));
        verifyNoInteractions(publisher, validador);
    }

    @Test
    void unaClaveDeOtroEnvioEsUnConflicto() {
        when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.of(envio));
        LogisticsEvent ajeno = evento(8L, "TF-OTRO", EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN,
                REGISTRO.plusSeconds(60));
        when(logisticsEvents.porEventId("evt-8")).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> admitir.ejecutar(comando(null, "evt-8")))
                .isInstanceOf(ClaveDeIdempotenciaEnUsoException.class)
                .hasMessageContaining("evt-8")
                .hasMessageContaining(TN);
        verifyNoInteractions(publisher);
    }

    @Test
    void unaClaveDemasiadoLargaSeRechaza() {
        when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.of(envio));
        String larga = "x".repeat(AdmitirEventoLogistico.LARGO_MAXIMO_CLAVE + 1);

        assertThatThrownBy(() -> admitir.ejecutar(comando(null, larga)))
                .isInstanceOf(ClaveDeIdempotenciaInvalidaException.class)
                .hasMessageContaining("100");
        verifyNoInteractions(publisher);
    }

    @Test
    void unaClaveEnBlancoSeIgnora() {
        envioValido();
        when(validador.validar(any(), any(), any(), any())).thenReturn(new LugarValidado(cdMedellin, MEDELLIN));

        EventoLogisticoEntrante evento = admitir.ejecutar(comando(null, "   "));

        assertThat(evento.eventId()).isNotBlank().isNotEqualTo("   ");
        verify(logisticsEvents, never()).porEventId(any());
    }

    @Test
    void unEnvioDesconocidoSeRechaza() {
        when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> admitir.ejecutar(comando(null, null)))
                .isInstanceOf(UnknownShipmentException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void unMovimientoEnElFuturoSeRechaza() {
        when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.of(envio));

        assertThatThrownBy(() -> admitir.ejecutar(comando(AHORA.plusSeconds(1), null)))
                .isInstanceOf(FechaDeMovimientoInvalidaException.class);
        verifyNoInteractions(publisher, validador);
    }

    @Test
    void siElRecorridoLoRechazaNoSePublica() {
        envioValido();
        when(validador.validar(any(), any(), any(), any())).thenThrow(new TransicionInvalidaException(TN,
                "DELIVERED", EventType.RECEIVED_AT_CENTER, Set.of()));

        assertThatThrownBy(() -> admitir.ejecutar(comando(null, null)))
                .isInstanceOf(TransicionInvalidaException.class);
        verifyNoInteractions(publisher);
    }
}
