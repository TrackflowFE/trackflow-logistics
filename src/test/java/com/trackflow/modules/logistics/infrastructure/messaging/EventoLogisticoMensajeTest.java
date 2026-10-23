package com.trackflow.modules.logistics.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import com.trackflow.modules.logistics.domain.EventType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EventoLogisticoMensajeTest {

    private static final Instant OCURRIDO = Instant.parse("2026-09-02T10:00:00Z");
    private static final EventoLogisticoEntrante ENTRANTE = new EventoLogisticoEntrante("evt-1", "TF-1",
            EventType.OUT_FOR_DELIVERY, 20L, 2L, "CD Bogotá", "BOGOTÁ D.C. - BOGOTÁ", "obs", "Ana", OCURRIDO);

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void idaYVueltaConservaTodosLosCampos() {
        EventoLogisticoMensaje mensaje = EventoLogisticoMensaje.from(ENTRANTE);

        assertThat(mensaje.type()).isEqualTo("OUT_FOR_DELIVERY");
        assertThat(mensaje.toEntrante()).isEqualTo(ENTRANTE);
    }

    @Test
    void viajaConLaVersionDelFormato() {
        String serializado = json.writeValueAsString(EventoLogisticoMensaje.from(ENTRANTE));

        assertThat(serializado).contains("\"version\":1").contains("\"type\":\"OUT_FOR_DELIVERY\"");
        assertThat(EventoLogisticoMensaje.from(ENTRANTE).schemaVersion()).isEqualTo(EventoLogisticoMensaje.VERSION);
    }

    @Test
    void entiendeLosNombresEnEspanolEIgnoraCamposDesconocidos() {
        String viejo = """
                {"eventId": "evt-2", "trackingNumber": "TF-1", "tipo": "DISPATCHED", "centroId": 10,
                 "ciudadId": 1, "punto": "CD Medellín", "ciudadNombre": "MEDELLÍN - ANTIOQUIA",
                 "observaciones": "x", "repartidorNombre": null, "ocurridoEn": "2026-09-02T10:00:00Z",
                 "version": 1, "campoNuevo": "algo"}
                """;

        EventoLogisticoEntrante entrante = json.readValue(viejo, EventoLogisticoMensaje.class).toEntrante();

        assertThat(entrante.tipo()).isEqualTo(EventType.DISPATCHED);
        assertThat(entrante.centroId()).isEqualTo(10L);
        assertThat(entrante.ciudadId()).isEqualTo(1L);
        assertThat(entrante.punto()).isEqualTo("CD Medellín");
        assertThat(entrante.observaciones()).isEqualTo("x");
        assertThat(entrante.ocurridoEn()).isEqualTo(OCURRIDO);
    }

    @Test
    void unTipoDesconocidoNoSePuedeConvertir() {
        EventoLogisticoMensaje mensaje = new EventoLogisticoMensaje("e", "TF-1", "TELEPORTED", 1L, 1L, "p", "c",
                null, null, OCURRIDO);

        assertThatThrownBy(mensaje::toEntrante).isInstanceOf(IllegalArgumentException.class);
    }
}
