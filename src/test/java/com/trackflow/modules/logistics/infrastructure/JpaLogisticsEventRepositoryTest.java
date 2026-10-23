package com.trackflow.modules.logistics.infrastructure;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaLogisticsEventRepositoryTest {

    @Mock
    private LogisticsEventJpaRepository jpa;

    @InjectMocks
    private JpaLogisticsEventRepository repositorio;

    private final LogisticsEvent evento = evento(1L, TN, EventType.RECEIVED_AT_CENTER,
            centro(10L, "CD Medellín", MEDELLIN), MEDELLIN, REGISTRO.plusSeconds(60));

    @Test
    void saveDevuelveLoQueGuardaLaBase() {
        when(jpa.save(evento)).thenReturn(evento);

        assertThat(repositorio.save(evento)).isSameAs(evento);
    }

    @Test
    void elHistorialSeOrdenaPorFechaYLuegoPorId() {
        when(jpa.findByTrackingNumberOrderByOccurredAtAscIdAsc(TN)).thenReturn(List.of(evento));

        assertThat(repositorio.findHistorial(TN)).containsExactly(evento);
    }

    @Test
    void todosCronologicamente() {
        when(jpa.findAllByOrderByOccurredAtAscIdAsc()).thenReturn(List.of(evento));

        assertThat(repositorio.findTodosCronologicamente()).containsExactly(evento);
    }

    @Test
    void buscaPorEventId() {
        when(jpa.existsByEventId("evt-1")).thenReturn(true);
        when(jpa.existsByEventId("otro")).thenReturn(false);
        when(jpa.findByEventId("evt-1")).thenReturn(Optional.of(evento));

        assertThat(repositorio.existePorEventId("evt-1")).isTrue();
        assertThat(repositorio.existePorEventId("otro")).isFalse();
        assertThat(repositorio.porEventId("evt-1")).containsSame(evento);
    }
}
