package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MANIZALES;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.envio;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.EstadoParada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.Parada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.RutaDelEnvio;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.TipoParada;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarRutaDelEnvioTest {

    private static final Instant T1 = REGISTRO.plusSeconds(3600);

    // exigir() es un método por defecto del repositorio: el mock debe ejecutarlo de verdad.
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TrackedShipmentRepository trackedShipments;

    @Mock
    private LogisticsEventRepository logisticsEvents;

    @Mock
    private ConsultarEstadoDelEnvio estadoDelEnvio;

    @Mock
    private CatalogoDeCiudades ciudades;

    @InjectMocks
    private ConsultarRutaDelEnvio consulta;

    private final TrackedShipment envio = envio(MEDELLIN, BOGOTA);
    private final Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);
    private final Centro cdManizales = centro(40L, "CD Manizales", MANIZALES);
    private final Centro cdBogota = centro(20L, "CD Bogotá", BOGOTA);

    @BeforeEach
    void envioConocido() {
        lenient().when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.of(envio));
    }

    private static EstadoDelEnvio estado(EventType ultimo, EventType penultimo, Ciudad actual, List<Ciudad> ruta) {
        Ciudad siguiente = ruta.size() == 1 ? ruta.get(0) : ruta.get(1);
        return new EstadoDelEnvio(TN, REGISTRO, ultimo, FlujoLogistico.estadoTras(ultimo, penultimo), null, null,
                null, actual, MEDELLIN, BOGOTA, ruta, siguiente,
                FlujoLogistico.siguientesPermitidos(ultimo, siguiente.equals(BOGOTA)));
    }

    private void conHistorial(List<LogisticsEvent> historial, EstadoDelEnvio estado) {
        when(logisticsEvents.findHistorial(TN)).thenReturn(historial);
        when(estadoDelEnvio.desdeHistorial(envio, historial)).thenReturn(estado);
    }

    @Test
    void sinMovimientosElOrigenEsLaActualYElRestoQuedaPendiente() {
        conHistorial(List.of(), estado(null, null, MEDELLIN, List.of(MEDELLIN, MANIZALES, BOGOTA)));

        RutaDelEnvio ruta = consulta.ejecutar(TN);

        assertThat(ruta.trackingNumber()).isEqualTo(TN);
        assertThat(ruta.estado()).isEqualTo("REGISTERED");
        assertThat(ruta.paradas()).extracting(Parada::ciudad, Parada::tipo, Parada::estado).containsExactly(
                tuple(MEDELLIN, TipoParada.ORIGEN, EstadoParada.ACTUAL),
                tuple(MANIZALES, TipoParada.ESCALA, EstadoParada.PENDIENTE),
                tuple(BOGOTA, TipoParada.DESTINO, EstadoParada.PENDIENTE));
        assertThat(ruta.siguienteParada()).isEqualTo(MANIZALES);
        assertThat(ruta.escalasPendientes()).isEqualTo(1);
    }

    @Test
    void enElHubElOrigenQuedaRecorridoYElHubEsLaActual() {
        List<LogisticsEvent> historial = List.of(
                evento(1L, TN, EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN, T1),
                evento(2L, TN, EventType.DISPATCHED, cdMedellin, MEDELLIN, T1.plusSeconds(60)),
                evento(3L, TN, EventType.RECEIVED_AT_CENTER, cdManizales, MANIZALES, T1.plusSeconds(120)));
        conHistorial(historial, estado(EventType.RECEIVED_AT_CENTER, EventType.DISPATCHED, MANIZALES,
                List.of(MANIZALES, BOGOTA)));
        when(ciudades.exigir(MANIZALES.id())).thenReturn(MANIZALES);

        RutaDelEnvio ruta = consulta.ejecutar(TN);

        assertThat(ruta.estado()).isEqualTo("IN_TRANSIT");
        assertThat(ruta.paradas()).extracting(Parada::ciudad, Parada::tipo, Parada::estado).containsExactly(
                tuple(MEDELLIN, TipoParada.ORIGEN, EstadoParada.RECORRIDA),
                tuple(MANIZALES, TipoParada.ESCALA, EstadoParada.ACTUAL),
                tuple(BOGOTA, TipoParada.DESTINO, EstadoParada.PENDIENTE));
        assertThat(ruta.siguienteParada()).isEqualTo(BOGOTA);
        assertThat(ruta.escalasPendientes()).isZero();
    }

    @Test
    void entregadoTodoQuedaRecorridoYNoHaySiguienteParada() {
        List<LogisticsEvent> historial = List.of(
                evento(1L, TN, EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN, T1),
                evento(2L, TN, EventType.DISPATCHED, cdMedellin, MEDELLIN, T1.plusSeconds(60)),
                evento(3L, TN, EventType.ARRIVED_AT_DESTINATION_CENTER, cdBogota, BOGOTA, T1.plusSeconds(120)),
                evento(4L, TN, EventType.OUT_FOR_DELIVERY, cdBogota, BOGOTA, T1.plusSeconds(180)),
                evento(5L, TN, EventType.DELIVERED, cdBogota, BOGOTA, T1.plusSeconds(240)));
        conHistorial(historial, estado(EventType.DELIVERED, EventType.OUT_FOR_DELIVERY, BOGOTA, List.of(BOGOTA)));
        when(ciudades.exigir(BOGOTA.id())).thenReturn(BOGOTA);

        RutaDelEnvio ruta = consulta.ejecutar(TN);

        assertThat(ruta.estado()).isEqualTo("DELIVERED");
        assertThat(ruta.paradas()).extracting(Parada::ciudad, Parada::tipo, Parada::estado).containsExactly(
                tuple(MEDELLIN, TipoParada.ORIGEN, EstadoParada.RECORRIDA),
                tuple(BOGOTA, TipoParada.DESTINO, EstadoParada.RECORRIDA));
        assertThat(ruta.siguienteParada()).isNull();
        assertThat(ruta.escalasPendientes()).isZero();
    }

    @Test
    void siLosEventosNoTienenCiudadSeAgregaLaActualAlFinalDeLoRecorrido() {
        List<LogisticsEvent> historial = List.of(evento(1L, TN, EventType.RECEIVED_AT_CENTER, null, null, T1));
        conHistorial(historial, estado(EventType.RECEIVED_AT_CENTER, EventType.DISPATCHED, MANIZALES,
                List.of(MANIZALES, BOGOTA)));

        RutaDelEnvio ruta = consulta.ejecutar(TN);

        assertThat(ruta.paradas()).extracting(Parada::ciudad).containsExactly(MEDELLIN, MANIZALES, BOGOTA);
        assertThat(ruta.paradas().get(1).estado()).isEqualTo(EstadoParada.ACTUAL);
    }

    @Test
    void elItinerarioNoSePuedeModificar() {
        conHistorial(List.of(), estado(null, null, MEDELLIN, List.of(MEDELLIN, BOGOTA)));

        RutaDelEnvio ruta = consulta.ejecutar(TN);

        assertThatThrownBy(() -> ruta.paradas().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void unEnvioDesconocidoFalla() {
        when(trackedShipments.porTrackingNumber("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consulta.ejecutar("X")).isInstanceOf(UnknownShipmentException.class);
    }
}
