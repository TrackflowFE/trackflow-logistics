package com.trackflow.modules.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LogisticsEventTest {

    private static final Instant AHORA = Instant.parse("2026-09-02T10:00:00Z");
    private static final LogisticsEvent.Lugar LUGAR = new LogisticsEvent.Lugar("CD Medellín", 10L, 1L,
            "MEDELLÍN - ANTIOQUIA");
    private static final LogisticsEvent.Parada PARADA = new LogisticsEvent.Parada(4L, "MANIZALES - CALDAS", 4);

    private static LogisticsEvent.Reporte reporte(EventType tipo, Instant ocurridoEn) {
        return new LogisticsEvent.Reporte("evt-1", "TF-0001", tipo, LUGAR, "frágil", "Ana", ocurridoEn);
    }

    @Test
    void registrarCopiaElReporteYLaFechaDeRegistro() {
        Instant ocurrido = AHORA.minusSeconds(3600);

        LogisticsEvent evento = LogisticsEvent.registrar(reporte(EventType.RECEIVED_AT_CENTER, ocurrido), AHORA);

        assertThat(evento.getId()).isNull();
        assertThat(evento.getEventId()).isEqualTo("evt-1");
        assertThat(evento.getTrackingNumber()).isEqualTo("TF-0001");
        assertThat(evento.getType()).isEqualTo(EventType.RECEIVED_AT_CENTER);
        assertThat(evento.getPoint()).isEqualTo("CD Medellín");
        assertThat(evento.getCenterId()).isEqualTo(10L);
        assertThat(evento.getCityId()).isEqualTo(1L);
        assertThat(evento.getCityName()).isEqualTo("MEDELLÍN - ANTIOQUIA");
        assertThat(evento.getNotes()).isEqualTo("frágil");
        assertThat(evento.getDelivererName()).isEqualTo("Ana");
        assertThat(evento.getOccurredAt()).isEqualTo(ocurrido);
        assertThat(evento.getRegisteredAt()).isEqualTo(AHORA);
    }

    @Test
    void unDespachoGuardaLaSiguienteParada() {
        LogisticsEvent evento = LogisticsEvent.registrar(reporte(EventType.DISPATCHED, AHORA), PARADA, AHORA);

        assertThat(evento.getNextStopCityId()).isEqualTo(4L);
        assertThat(evento.getNextStopCity()).isEqualTo("MANIZALES - CALDAS");
        assertThat(evento.getPlannedLegHours()).isEqualTo(4);
    }

    @Test
    void losDemasMovimientosDescartanLaSiguienteParada() {
        LogisticsEvent evento = LogisticsEvent.registrar(reporte(EventType.RECEIVED_AT_CENTER, AHORA), PARADA,
                AHORA);

        assertThat(evento.getNextStopCityId()).isNull();
        assertThat(evento.getNextStopCity()).isNull();
        assertThat(evento.getPlannedLegHours()).isNull();
    }

    @Test
    void unDespachoSinParadaNoTieneSiguienteCiudad() {
        LogisticsEvent evento = LogisticsEvent.registrar(reporte(EventType.DISPATCHED, AHORA), null, AHORA);

        assertThat(evento.getNextStopCityId()).isNull();
    }

    @Test
    void rechazaUnMovimientoEnElFuturo() {
        Instant futuro = AHORA.plusSeconds(1);

        assertThatThrownBy(() -> LogisticsEvent.registrar(reporte(EventType.DISPATCHED, futuro), AHORA))
                .isInstanceOf(FechaDeMovimientoInvalidaException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void rechazaUnMovimientoSinFecha() {
        assertThatThrownBy(() -> LogisticsEvent.registrar(reporte(EventType.DISPATCHED, null), AHORA))
                .isInstanceOf(FechaDeMovimientoInvalidaException.class)
                .hasMessage("Falta la fecha en que ocurrió el movimiento");
    }
}
