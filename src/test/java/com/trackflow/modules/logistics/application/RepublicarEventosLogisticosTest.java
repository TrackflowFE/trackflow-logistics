package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.MANIZALES;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.shared.events.DomainEvent;
import com.trackflow.shared.events.EventPublisher;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepublicarEventosLogisticosTest {

    private static final Instant AHORA = Instant.parse("2026-10-01T00:00:00Z");

    @Mock
    private LogisticsEventRepository logisticsEvents;

    @Mock
    private EventPublisher events;

    @Test
    void republicaEnOrdenYCalculaElEstadoSegunElMovimientoAnteriorDelMismoEnvio() {
        Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);
        Centro cdManizales = centro(40L, "CD Manizales", MANIZALES);
        Instant t = REGISTRO.plusSeconds(60);
        when(logisticsEvents.findTodosCronologicamente()).thenReturn(List.of(
                evento(1L, "TF-A", EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN, t),
                evento(2L, "TF-B", EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN, t.plusSeconds(1)),
                evento(3L, "TF-A", EventType.DISPATCHED, cdMedellin, MEDELLIN, t.plusSeconds(2)),
                evento(4L, "TF-A", EventType.RECEIVED_AT_CENTER, cdManizales, MANIZALES, t.plusSeconds(3))));
        RepublicarEventosLogisticos republicar = new RepublicarEventosLogisticos(logisticsEvents, events,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        int total = republicar.ejecutar();

        assertThat(total).isEqualTo(4);
        ArgumentCaptor<DomainEvent> publicados = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events, times(4)).publish(publicados.capture());
        List<EventoLogisticoRegistradoEvent> eventos = publicados.getAllValues().stream()
                .map(EventoLogisticoRegistradoEvent.class::cast).toList();
        assertThat(eventos).extracting(EventoLogisticoRegistradoEvent::logisticsEventId)
                .containsExactly(1L, 2L, 3L, 4L);
        // El primer ingreso de cada envío deja AT_DISTRIBUTION_CENTER; el ingreso al hub, IN_TRANSIT.
        assertThat(eventos).extracting(EventoLogisticoRegistradoEvent::resultingStatus)
                .containsExactly("AT_DISTRIBUTION_CENTER", "AT_DISTRIBUTION_CENTER", "IN_TRANSIT", "IN_TRANSIT");
        assertThat(eventos).allSatisfy(e -> assertThat(e.occurredAt()).isEqualTo(AHORA));
        assertThat(eventos.get(3).cityId()).isEqualTo(MANIZALES.id());
        assertThat(eventos.get(3).movedAt()).isEqualTo(t.plusSeconds(3));
    }

    @Test
    void sinEventosNoPublicaNada() {
        when(logisticsEvents.findTodosCronologicamente()).thenReturn(List.of());

        int total = new RepublicarEventosLogisticos(logisticsEvents, events, Clock.systemUTC()).ejecutar();

        assertThat(total).isZero();
        verifyNoInteractions(events);
    }
}
