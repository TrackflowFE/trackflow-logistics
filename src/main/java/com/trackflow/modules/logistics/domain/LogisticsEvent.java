package com.trackflow.modules.logistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "logistics_events")
public class LogisticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del mensaje de ingesta. Unico para descartar reentregas del broker. */
    @Column(nullable = false, unique = true, updatable = false)
    private String eventId;

    @Column(nullable = false, updatable = false)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EventType type;

    @Column(nullable = false, updatable = false)
    private String point;

    private String notes;

    /**
     * Id del centro del catálogo, si el evento se reportó con uno. Nullable a
     * propósito: eventos históricos y los que usan el respaldo de texto libre no
     * tienen centro asociado. No se resuelve como referencia viva — si el centro se
     * renombra o desactiva después, este evento no cambia.
     */
    @Column(updatable = false)
    private Long centerId;

    /**
     * Ciudad del centro, por id del catálogo: es la que sirve para agrupar por ciudad.
     * Null en los eventos históricos sin centro.
     */
    @Column(updatable = false)
    private Long cityId;

    /** Nombre de la ciudad del centro, congelado en el momento del registro. */
    @Column(updatable = false)
    private String cityName;

    /**
     * Hacia qué ciudad salió el camión: la siguiente parada de la ruta en el momento del
     * despacho. Solo tiene valor en eventos DISPATCHED. Sin este dato el historial solo
     * podía decir "despachado hacia" el destino final, aunque el camión fuera a un hub.
     */
    @Column(updatable = false)
    private Long nextStopCityId;

    /** Nombre de esa ciudad, congelado en el momento del despacho. */
    @Column(updatable = false)
    private String nextStopCity;

    /**
     * Cuántas horas dura, según la red, el tramo que empieza con este despacho. Es la
     * referencia para saber si el camión va retrasado. Null si no hay tramo en la red
     * (por ejemplo, un trayecto directo asumido porque se cerró la ruta).
     */
    @Column(updatable = false)
    private Integer plannedLegHours;

    /** Quien reparte. Solo tiene valor en eventos OUT_FOR_DELIVERY. */
    @Column(updatable = false)
    private String delivererName;

    /** Cuándo ocurrió el movimiento. Es la fecha que ve el cliente y la que ordena el historial. */
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    /** Cuándo se recibió el reporte. Sirve para auditar cuánto tardó en reportarse. */
    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    /**
     * Dónde ocurrió el movimiento.
     *
     * @param centerId id del centro del catálogo, o null si se reportó con texto libre
     * @param cityId id de la ciudad del centro en el catálogo
     * @param cityName ciudad del centro, congelada en el momento del registro
     */
    public record Lugar(String point, Long centerId, Long cityId, String cityName) {
    }

    /**
     * Ciudad a la que se dirige un despacho y cuánto dura el tramo hasta ella.
     *
     * @param horasDelTramo duración planeada del tramo; null si la red no lo tiene
     */
    public record Parada(Long cityId, String cityName, Integer horasDelTramo) {
    }

    /** Lo que reporta el operador, antes de que el sistema le ponga fecha de registro. */
    public record Reporte(String eventId, String trackingNumber, EventType type, Lugar lugar, String notes,
            String delivererName, Instant occurredAt) {
    }

    protected LogisticsEvent() {
    }

    private LogisticsEvent(Reporte reporte, Parada siguienteParada, Instant registeredAt) {
        this.eventId = reporte.eventId();
        this.trackingNumber = reporte.trackingNumber();
        this.type = reporte.type();
        this.point = reporte.lugar().point();
        this.notes = reporte.notes();
        this.centerId = reporte.lugar().centerId();
        this.cityId = reporte.lugar().cityId();
        this.cityName = reporte.lugar().cityName();
        this.delivererName = reporte.delivererName();
        this.occurredAt = reporte.occurredAt();
        this.registeredAt = registeredAt;
        if (siguienteParada != null) {
            this.nextStopCityId = siguienteParada.cityId();
            this.nextStopCity = siguienteParada.cityName();
            this.plannedLegHours = siguienteParada.horasDelTramo();
        }
    }

    /**
     * Un movimiento no puede haber ocurrido después de reportarse. Se comprueba aquí
     * y no en el DTO porque el reporte llega por REST y también por la cola.
     */
    public static LogisticsEvent registrar(Reporte reporte, Instant registeredAt) {
        return registrar(reporte, null, registeredAt);
    }

    /**
     * @param siguienteParada hacia dónde va el camión, para un despacho; null en el resto
     */
    public static LogisticsEvent registrar(Reporte reporte, Parada siguienteParada, Instant registeredAt) {
        Instant occurredAt = reporte.occurredAt();
        if (occurredAt == null || occurredAt.isAfter(registeredAt)) {
            throw new FechaDeMovimientoInvalidaException(occurredAt, registeredAt);
        }

        return new LogisticsEvent(reporte, reporte.type() == EventType.DISPATCHED ? siguienteParada : null,
                registeredAt);
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public EventType getType() {
        return type;
    }

    public String getPoint() {
        return point;
    }

    public String getNotes() {
        return notes;
    }

    public Long getCenterId() {
        return centerId;
    }

    public Long getCityId() {
        return cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public Long getNextStopCityId() {
        return nextStopCityId;
    }

    public String getNextStopCity() {
        return nextStopCity;
    }

    public Integer getPlannedLegHours() {
        return plannedLegHours;
    }

    public String getDelivererName() {
        return delivererName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
