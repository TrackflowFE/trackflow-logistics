package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MANIZALES;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.envio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.application.ValidadorDeRecorrido.LugarValidado;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.Tramo;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.events.DomainEvent;
import com.trackflow.shared.events.EventPublisher;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrarEventoLogisticoTest {

    private static final Instant AHORA = Instant.parse("2026-09-02T12:00:00Z");
    private static final Instant OCURRIDO = AHORA.minusSeconds(300);

    @Mock
    private LogisticsEventRepository logisticsEvents;

    @Mock
    private TrackedShipmentRepository trackedShipments;

    @Mock
    private ConsultarEstadoDelEnvio estadoDelEnvio;

    @Mock
    private ValidadorDeRecorrido validador;

    @Mock
    private RedDeRutas red;

    @Mock
    private EventPublisher events;

    private RegistrarEventoLogistico registrar;

    private final TrackedShipment envio = envio(MEDELLIN, BOGOTA);
    private final Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);

    @BeforeEach
    void setUp() {
        registrar = new RegistrarEventoLogistico(logisticsEvents, trackedShipments, estadoDelEnvio, validador, red,
                events, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static EventoLogisticoEntrante entrante(EventType tipo, Instant ocurridoEn) {
        // El punto y la ciudad del mensaje son falsos a propósito: deben tomarse del catálogo.
        return new EventoLogisticoEntrante("evt-1", TN, tipo, 10L, 99L, "texto del productor", "CIUDAD FALSA",
                "obs", null, ocurridoEn);
    }

    /** En Medellín, recibido en el centro, con ruta Medellín → Manizales → Bogotá. */
    private static EstadoDelEnvio recibidoEnMedellin() {
        Ciudad siguiente = MANIZALES;
        return new EstadoDelEnvio(TN, REGISTRO, EventType.RECEIVED_AT_CENTER, "AT_DISTRIBUTION_CENTER",
                REGISTRO.plusSeconds(60), 10L, "CD Medellín", MEDELLIN, MEDELLIN, BOGOTA,
                List.of(MEDELLIN, MANIZALES, BOGOTA), siguiente,
                FlujoLogistico.siguientesPermitidos(EventType.RECEIVED_AT_CENTER, false));
    }

    private void flujoValido(EventType tipo) {
        when(logisticsEvents.existePorEventId("evt-1")).thenReturn(false);
        when(trackedShipments.porTrackingNumberParaActualizar(TN)).thenReturn(Optional.of(envio));
        when(logisticsEvents.findHistorial(TN)).thenReturn(List.of());
        when(estadoDelEnvio.desdeHistorial(envio, List.of())).thenReturn(recibidoEnMedellin());
        when(validador.validar(any(), any(), any(), any())).thenReturn(new LugarValidado(cdMedellin, MEDELLIN));
        when(logisticsEvents.save(any())).thenAnswer(inv -> {
            LogisticsEvent evento = inv.getArgument(0);
            ReflectionTestUtils.setField(evento, "id", 55L);
            return evento;
        });
    }

    @Test
    void registraUnDespachoConLaSiguienteParadaYLasHorasDelTramo() {
        flujoValido(EventType.DISPATCHED);
        when(red.tramosActivos()).thenReturn(List.of(new Tramo(1L, 2L, 12), new Tramo(1L, 4L, 4)));

        Optional<LogisticsEvent> guardado = registrar.ejecutar(entrante(EventType.DISPATCHED, OCURRIDO));

        assertThat(guardado).isPresent();
        LogisticsEvent evento = guardado.get();
        assertThat(evento.getType()).isEqualTo(EventType.DISPATCHED);
        assertThat(evento.getPoint()).isEqualTo("CD Medellín");
        assertThat(evento.getCityId()).isEqualTo(MEDELLIN.id());
        assertThat(evento.getCityName()).isEqualTo(MEDELLIN.etiqueta());
        assertThat(evento.getNextStopCityId()).isEqualTo(MANIZALES.id());
        assertThat(evento.getNextStopCity()).isEqualTo(MANIZALES.etiqueta());
        assertThat(evento.getPlannedLegHours()).isEqualTo(4);
        assertThat(evento.getRegisteredAt()).isEqualTo(AHORA);
        assertThat(evento.getOccurredAt()).isEqualTo(OCURRIDO);

        ArgumentCaptor<DomainEvent> publicado = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(publicado.capture());
        EventoLogisticoRegistradoEvent integracion = (EventoLogisticoRegistradoEvent) publicado.getValue();
        assertThat(integracion.logisticsEventId()).isEqualTo(55L);
        assertThat(integracion.eventType()).isEqualTo("DISPATCHED");
        assertThat(integracion.resultingStatus()).isEqualTo("IN_TRANSIT");
        assertThat(integracion.centerId()).isEqualTo(10L);
        assertThat(integracion.movedAt()).isEqualTo(OCURRIDO);
        assertThat(integracion.occurredAt()).isEqualTo(AHORA);
        assertThat(integracion.nextStopCityId()).isEqualTo(MANIZALES.id());
        assertThat(integracion.plannedLegHours()).isEqualTo(4);
    }

    @Test
    void sinTramoDirectoEnLaRedLasHorasQuedanNulas() {
        flujoValido(EventType.DISPATCHED);
        when(red.tramosActivos()).thenReturn(List.of(new Tramo(1L, 2L, 12)));

        LogisticsEvent evento = registrar.ejecutar(entrante(EventType.DISPATCHED, OCURRIDO)).orElseThrow();

        assertThat(evento.getNextStopCityId()).isEqualTo(MANIZALES.id());
        assertThat(evento.getPlannedLegHours()).isNull();
    }

    @Test
    void unaReentregaDelBrokerSeDescartaSinTocarNada() {
        when(logisticsEvents.existePorEventId("evt-1")).thenReturn(true);

        Optional<LogisticsEvent> resultado = registrar.ejecutar(entrante(EventType.DISPATCHED, OCURRIDO));

        assertThat(resultado).isEmpty();
        verify(logisticsEvents, never()).save(any());
        verifyNoInteractions(trackedShipments, events);
    }

    @Test
    void unEnvioDesconocidoSeRechaza() {
        when(trackedShipments.porTrackingNumberParaActualizar(TN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrar.ejecutar(entrante(EventType.DISPATCHED, OCURRIDO)))
                .isInstanceOf(UnknownShipmentException.class);
        verifyNoInteractions(events);
    }

    @Test
    void unMensajeSinFechaSeRechaza() {
        when(trackedShipments.porTrackingNumberParaActualizar(TN)).thenReturn(Optional.of(envio));

        assertThatThrownBy(() -> registrar.ejecutar(entrante(EventType.DISPATCHED, null)))
                .isInstanceOf(FechaDeMovimientoInvalidaException.class);
        verify(logisticsEvents, never()).save(any());
    }

    @Test
    void unMensajeConFechaFuturaSeRechaza() {
        when(trackedShipments.porTrackingNumberParaActualizar(TN)).thenReturn(Optional.of(envio));

        assertThatThrownBy(() -> registrar.ejecutar(entrante(EventType.DISPATCHED, AHORA.plusSeconds(5))))
                .isInstanceOf(FechaDeMovimientoInvalidaException.class);
        verify(logisticsEvents, never()).save(any());
    }

    @Test
    void siLasReglasDelRecorridoLoRechazanNoSeGuardaNiSePublica() {
        when(trackedShipments.porTrackingNumberParaActualizar(TN)).thenReturn(Optional.of(envio));
        when(logisticsEvents.findHistorial(TN)).thenReturn(List.of());
        when(estadoDelEnvio.desdeHistorial(envio, List.of())).thenReturn(recibidoEnMedellin());
        when(validador.validar(any(), any(), any(), any()))
                .thenThrow(new MovimientoFueraDeOrdenException(TN, OCURRIDO, AHORA));

        assertThatThrownBy(() -> registrar.ejecutar(entrante(EventType.DISPATCHED, OCURRIDO)))
                .isInstanceOf(MovimientoFueraDeOrdenException.class);
        verify(logisticsEvents, never()).save(any());
        verifyNoInteractions(events);
    }
}
