package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.CALI;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MANIZALES;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.envio;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.Tramo;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarEstadoDelEnvioTest {

    private static final Map<Long, Ciudad> CATALOGO = Map.of(1L, MEDELLIN, 2L, BOGOTA, 3L, CALI, 4L, MANIZALES);
    private static final Instant T1 = REGISTRO.plusSeconds(3600);

    // exigir() es un método por defecto del repositorio: el mock debe ejecutarlo de verdad.
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TrackedShipmentRepository trackedShipments;

    @Mock
    private LogisticsEventRepository logisticsEvents;

    @Mock
    private CatalogoDeCentros centros;

    @Mock
    private CatalogoDeCiudades ciudades;

    @Mock
    private RedDeRutas red;

    private ConsultarEstadoDelEnvio consulta;

    private final Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);
    private final Centro cdManizales = centro(40L, "CD Manizales", MANIZALES);
    private final Centro cdBogota = centro(20L, "CD Bogotá", BOGOTA);

    @BeforeEach
    void setUp() {
        consulta = new ConsultarEstadoDelEnvio(trackedShipments, logisticsEvents, centros, ciudades,
                new PlanificarRuta(red));
        lenient().when(ciudades.exigir(anyLong())).thenAnswer(inv -> CATALOGO.get(inv.<Long>getArgument(0)));
        // Medellín → Manizales → Bogotá es más rápido que el directo.
        lenient().when(red.tramosActivos()).thenReturn(List.of(
                new Tramo(1L, 4L, 4), new Tramo(4L, 2L, 5), new Tramo(1L, 2L, 12)));
        lenient().when(centros.porId(10L)).thenReturn(Optional.of(cdMedellin));
        lenient().when(centros.porId(40L)).thenReturn(Optional.of(cdManizales));
        lenient().when(centros.porId(20L)).thenReturn(Optional.of(cdBogota));
    }

    @Test
    void unEnvioDesconocidoFalla() {
        when(trackedShipments.porTrackingNumber("NO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consulta.ejecutar("NO-EXISTE"))
                .isInstanceOf(UnknownShipmentException.class)
                .hasMessageContaining("NO-EXISTE");
        verify(logisticsEvents, never()).findHistorial(any());
    }

    @Test
    void sinMovimientosEstaRegistradoEnElOrigenConLaRutaPlaneada() {
        when(trackedShipments.porTrackingNumber(TN)).thenReturn(Optional.of(envio(MEDELLIN, BOGOTA)));
        when(logisticsEvents.findHistorial(TN)).thenReturn(List.of());

        EstadoDelEnvio estado = consulta.ejecutar(TN);

        assertThat(estado.trackingNumber()).isEqualTo(TN);
        assertThat(estado.registradoEn()).isEqualTo(REGISTRO);
        assertThat(estado.estado()).isEqualTo(FlujoLogistico.ESTADO_REGISTRADO);
        assertThat(estado.ultimoTipo()).isNull();
        assertThat(estado.ultimoMovimientoEn()).isNull();
        assertThat(estado.centroActualId()).isNull();
        assertThat(estado.centroActualNombre()).isNull();
        assertThat(estado.ciudadActual()).isEqualTo(MEDELLIN);
        assertThat(estado.rutaPlaneada()).containsExactly(MEDELLIN, MANIZALES, BOGOTA);
        assertThat(estado.siguienteParada()).isEqualTo(MANIZALES);
        assertThat(estado.siguientesPermitidos()).containsExactly(EventType.RECEIVED_AT_CENTER);
    }

    @Test
    void despachadoHaciaUnHubSoloAdmiteElIngresoAlHub() {
        List<LogisticsEvent> historial = List.of(
                evento(1L, TN, EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN, T1),
                evento(2L, TN, EventType.DISPATCHED, cdMedellin, MEDELLIN, T1.plusSeconds(60)));

        EstadoDelEnvio estado = consulta.desdeHistorial(envio(MEDELLIN, BOGOTA), historial);

        assertThat(estado.ultimoTipo()).isEqualTo(EventType.DISPATCHED);
        assertThat(estado.estado()).isEqualTo("IN_TRANSIT");
        assertThat(estado.ultimoMovimientoEn()).isEqualTo(T1.plusSeconds(60));
        assertThat(estado.centroActualId()).isEqualTo(10L);
        assertThat(estado.centroActualNombre()).isEqualTo("CD Medellín");
        assertThat(estado.siguienteParada()).isEqualTo(MANIZALES);
        assertThat(estado.siguientesPermitidos()).containsExactly(EventType.RECEIVED_AT_CENTER);
    }

    @Test
    void enElHubElIngresoDejaEnTransitoYLaSiguienteParadaEsElDestino() {
        List<LogisticsEvent> historial = List.of(
                evento(1L, TN, EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN, T1),
                evento(2L, TN, EventType.DISPATCHED, cdMedellin, MEDELLIN, T1.plusSeconds(60)),
                evento(3L, TN, EventType.RECEIVED_AT_CENTER, cdManizales, MANIZALES, T1.plusSeconds(120)),
                evento(4L, TN, EventType.DISPATCHED, cdManizales, MANIZALES, T1.plusSeconds(180)));

        EstadoDelEnvio estado = consulta.desdeHistorial(envio(MEDELLIN, BOGOTA), historial);

        assertThat(estado.ciudadActual()).isEqualTo(MANIZALES);
        assertThat(estado.rutaPlaneada()).containsExactly(MANIZALES, BOGOTA);
        assertThat(estado.siguienteParada()).isEqualTo(BOGOTA);
        assertThat(estado.siguientesPermitidos()).containsExactly(EventType.ARRIVED_AT_DESTINATION_CENTER);
        assertThat(estado.ciudadQueExige(CiudadEsperada.ORIGEN)).isEqualTo(MEDELLIN);
        assertThat(estado.ciudadQueExige(CiudadEsperada.DESTINO)).isEqualTo(BOGOTA);
        assertThat(estado.ciudadQueExige(CiudadEsperada.ACTUAL)).isEqualTo(MANIZALES);
        assertThat(estado.ciudadQueExige(CiudadEsperada.SIGUIENTE_PARADA)).isEqualTo(BOGOTA);
    }

    @Test
    void enElDestinoLaRutaEsSoloElDestino() {
        List<LogisticsEvent> historial = List.of(
                evento(5L, TN, EventType.ARRIVED_AT_DESTINATION_CENTER, cdBogota, BOGOTA, T1));

        EstadoDelEnvio estado = consulta.desdeHistorial(envio(MEDELLIN, BOGOTA), historial);

        assertThat(estado.rutaPlaneada()).containsExactly(BOGOTA);
        assertThat(estado.siguienteParada()).isEqualTo(BOGOTA);
        assertThat(estado.siguientesPermitidos()).containsExactly(EventType.OUT_FOR_DELIVERY);
    }

    @Test
    void unEventoViejoSinCentroDejaElPaqueteEnElOrigen() {
        List<LogisticsEvent> historial = List.of(evento(1L, TN, EventType.RECEIVED_AT_CENTER, null, null, T1));

        EstadoDelEnvio estado = consulta.desdeHistorial(envio(MEDELLIN, BOGOTA), historial);

        assertThat(estado.ciudadActual()).isEqualTo(MEDELLIN);
        assertThat(estado.centroActualId()).isNull();
        verify(centros, never()).porId(any());
    }

    @Test
    void sinRutaEnLaRedAsumeElTrayectoDirecto() {
        when(red.tramosActivos()).thenReturn(List.of());

        EstadoDelEnvio estado = consulta.desdeHistorial(envio(MEDELLIN, CALI), List.of());

        assertThat(estado.rutaPlaneada()).containsExactly(MEDELLIN, CALI);
        assertThat(estado.siguienteParada()).isEqualTo(CALI);
    }

    @Test
    void sinRutaYYaEnElDestinoLaRutaEsElDestino() {
        when(red.tramosActivos()).thenReturn(List.of());
        // Envío con origen igual a destino: el planificador devuelve la ruta de una ciudad.
        EstadoDelEnvio estado = consulta.desdeHistorial(envio(CALI, CALI), List.of());

        assertThat(estado.rutaPlaneada()).containsExactly(CALI);
    }
}
