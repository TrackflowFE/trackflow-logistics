package com.trackflow.modules.logistics.domain;

import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Datos de prueba compartidos. Centro y LogisticsEvent son entidades JPA sin setters
 * (solo las carga la base), así que aquí se arman como las dejaría Hibernate.
 *
 * Red de ejemplo: Medellín (1), Bogotá (2), Manizales (4) y Cali (3).
 */
public final class DatosDePrueba {

    public static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");
    public static final Ciudad BOGOTA = new Ciudad(2L, "BOGOTÁ D.C.", "BOGOTÁ");
    public static final Ciudad CALI = new Ciudad(3L, "CALI", "VALLE DEL CAUCA");
    public static final Ciudad MANIZALES = new Ciudad(4L, "MANIZALES", "CALDAS");

    public static final String TN = "TF-0001";
    public static final Instant REGISTRO = Instant.parse("2026-09-01T08:00:00Z");

    private DatosDePrueba() {
    }

    public static Centro centro(Long id, String nombre, TipoCentro tipo, Long ciudadId, boolean activo) {
        Centro centro = new Centro();
        ReflectionTestUtils.setField(centro, "id", id);
        ReflectionTestUtils.setField(centro, "name", nombre);
        ReflectionTestUtils.setField(centro, "type", tipo);
        ReflectionTestUtils.setField(centro, "cityId", ciudadId);
        ReflectionTestUtils.setField(centro, "address", "Calle " + id);
        ReflectionTestUtils.setField(centro, "active", activo);
        return centro;
    }

    public static Centro centro(Long id, String nombre, Ciudad ciudad) {
        return centro(id, nombre, TipoCentro.CENTRO_DISTRIBUCION, ciudad.id(), true);
    }

    /** Un movimiento ya guardado (con id), en el centro indicado. */
    public static LogisticsEvent evento(Long id, String trackingNumber, EventType tipo, Centro centro,
            Ciudad ciudad, Instant ocurridoEn) {
        LogisticsEvent evento = LogisticsEvent.registrar(new LogisticsEvent.Reporte(
                "evt-" + id,
                trackingNumber,
                tipo,
                new LogisticsEvent.Lugar(centro == null ? "punto libre" : centro.getName(),
                        centro == null ? null : centro.getId(),
                        ciudad == null ? null : ciudad.id(),
                        ciudad == null ? null : ciudad.etiqueta()),
                null,
                tipo == EventType.OUT_FOR_DELIVERY ? "Repartidor" : null,
                ocurridoEn), ocurridoEn.plusSeconds(60));
        ReflectionTestUtils.setField(evento, "id", id);
        return evento;
    }

    public static TrackedShipment envio(Ciudad origen, Ciudad destino) {
        return new TrackedShipment(TN, REGISTRO, origen.id(), destino.id());
    }
}
