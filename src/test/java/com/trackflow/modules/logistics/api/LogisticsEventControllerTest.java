package com.trackflow.modules.logistics.api;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MANIZALES;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.bootstrap.ValidationExceptionHandler;
import com.trackflow.modules.logistics.application.AdmitirEventoLogistico;
import com.trackflow.modules.logistics.application.ConsultarAccionesDisponibles;
import com.trackflow.modules.logistics.application.ConsultarAccionesDisponibles.Accion;
import com.trackflow.modules.logistics.application.ConsultarAccionesDisponibles.Acciones;
import com.trackflow.modules.logistics.application.ConsultarAccionesDisponibles.CentroDisponible;
import com.trackflow.modules.logistics.application.ConsultarHistorial;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.EstadoParada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.Parada;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.RutaDelEnvio;
import com.trackflow.modules.logistics.application.ConsultarRutaDelEnvio.TipoParada;
import com.trackflow.modules.logistics.application.EstadoDelEnvio;
import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CentroFueraDeCiudadException;
import com.trackflow.modules.logistics.domain.CentroInvalidoException;
import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaEnUsoException;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaInvalidaException;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.MovimientoAntesDelRegistroException;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.TipoCentro;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LogisticsEventControllerTest {

    private static final Instant OCURRIDO = Instant.parse("2026-09-02T10:00:00Z");

    private AdmitirEventoLogistico admitir;
    private ConsultarHistorial historial;
    private ConsultarAccionesDisponibles acciones;
    private ConsultarRutaDelEnvio ruta;
    private MockMvc mvc;

    private final Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);

    @BeforeEach
    void setUp() {
        admitir = mock(AdmitirEventoLogistico.class);
        historial = mock(ConsultarHistorial.class);
        acciones = mock(ConsultarAccionesDisponibles.class);
        ruta = mock(ConsultarRutaDelEnvio.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new LogisticsEventController(admitir, historial, acciones, ruta))
                .setControllerAdvice(new ValidationExceptionHandler(), new LogisticsExceptionHandler())
                .build();
    }

    private static String cuerpo(String type, Long centerId, String delivererName) {
        return """
                {"type": %s, "centerId": %s, "notes": "frágil", "delivererName": %s,
                 "occurredAt": "2026-09-02T10:00:00Z"}
                """.formatted(type == null ? "null" : '"' + type + '"', centerId,
                delivererName == null ? "null" : '"' + delivererName + '"');
    }

    @Test
    void admitirResponde202ConElEventoEncolado() throws Exception {
        when(admitir.ejecutar(any())).thenReturn(new EventoLogisticoEntrante("evt-1", TN,
                EventType.RECEIVED_AT_CENTER, 10L, 1L, "CD Medellín", MEDELLIN.etiqueta(), "frágil", null, OCURRIDO));

        mvc.perform(post("/api/shipments/{tn}/events", TN)
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("RECEIVED_AT_CENTER", 10L, null)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.trackingNumber").value(TN))
                .andExpect(jsonPath("$.type").value("RECEIVED_AT_CENTER"))
                .andExpect(jsonPath("$.centerId").value(10))
                .andExpect(jsonPath("$.point").value("CD Medellín"))
                .andExpect(jsonPath("$.processingStatus").value("QUEUED"));

        ArgumentCaptor<AdmitirEventoLogistico.Command> comando =
                ArgumentCaptor.forClass(AdmitirEventoLogistico.Command.class);
        verify(admitir).ejecutar(comando.capture());
        assertThat(comando.getValue().trackingNumber()).isEqualTo(TN);
        assertThat(comando.getValue().tipo()).isEqualTo(EventType.RECEIVED_AT_CENTER);
        assertThat(comando.getValue().centroId()).isEqualTo(10L);
        assertThat(comando.getValue().observaciones()).isEqualTo("frágil");
        assertThat(comando.getValue().ocurridoEn()).isEqualTo(OCURRIDO);
        assertThat(comando.getValue().claveIdempotencia()).isEqualTo("clave-1");
    }

    @Test
    void sinTipoNiCentroDetallaLosCamposFaltantes() throws Exception {
        mvc.perform(post("/api/shipments/{tn}/events", TN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos obligatorios incompletos"))
                .andExpect(jsonPath("$.missingFields.type").value("el tipo de evento es obligatorio"))
                .andExpect(jsonPath("$.missingFields.centerId").value("el centro es obligatorio"));
        verifyNoInteractions(admitir);
    }

    @Test
    void salidaARepartoSinRepartidorSeRechaza() throws Exception {
        mvc.perform(post("/api/shipments/{tn}/events", TN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("OUT_FOR_DELIVERY", 20L, "  ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.missingFields.delivererPresentWhenRequired")
                        .value("el repartidor es obligatorio para un evento OUT_FOR_DELIVERY"));
        verifyNoInteractions(admitir);
    }

    @Test
    void unTipoFueraDelCatalogoEsUnValorNoAdmitido() throws Exception {
        mvc.perform(post("/api/shipments/{tn}/events", TN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("TELEPORTED", 10L, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Valor no admitido"))
                .andExpect(jsonPath("$.cause").value(containsString("TELEPORTED")));
        verifyNoInteractions(admitir);
    }

    static Stream<Arguments> erroresDelDominio() {
        return Stream.of(
                Arguments.of(new UnknownShipmentException(TN), 404, null),
                Arguments.of(new FechaDeMovimientoInvalidaException(null, OCURRIDO), 400,
                        "Fecha del movimiento inválida"),
                Arguments.of(CentroInvalidoException.noExiste(9L), 422, "Centro inválido"),
                Arguments.of(CentroFueraDeCiudadException.para(EventType.DELIVERED, CiudadEsperada.DESTINO, "CD",
                        "A", "B"), 422, "El centro no está en la ciudad esperada"),
                Arguments.of(new ClaveDeIdempotenciaInvalidaException(100), 400, "Clave de idempotencia inválida"),
                Arguments.of(new ClaveDeIdempotenciaEnUsoException("k", TN), 409,
                        "Clave de idempotencia ya usada en otro envío"),
                Arguments.of(new TransicionInvalidaException(TN, "DELIVERED", EventType.DISPATCHED, Set.of()), 422,
                        "Movimiento fuera del recorrido"),
                Arguments.of(new MovimientoFueraDeOrdenException(TN, OCURRIDO, OCURRIDO), 422,
                        "Movimiento anterior al último registrado"),
                Arguments.of(new MovimientoAntesDelRegistroException(TN, OCURRIDO, OCURRIDO), 422,
                        "Movimiento anterior al registro del envío"));
    }

    @ParameterizedTest
    @MethodSource("erroresDelDominio")
    void traduceLasExcepcionesDelDominioACodigosHttp(RuntimeException error, int codigo, String titulo)
            throws Exception {
        when(admitir.ejecutar(any())).thenThrow(error);

        var resultado = mvc.perform(post("/api/shipments/{tn}/events", TN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("DISPATCHED", 10L, null)))
                .andExpect(status().is(codigo))
                .andExpect(jsonPath("$.detail").value(error.getMessage()));
        if (titulo != null) {
            resultado.andExpect(jsonPath("$.title").value(titulo));
        }
    }

    @Test
    void historialDeUnEnvioSinMovimientosLoExplica() throws Exception {
        when(historial.ejecutar(TN)).thenReturn(List.of());

        mvc.perform(get("/api/shipments/{tn}/events", TN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value(TN))
                .andExpect(jsonPath("$.totalMovements").value(0))
                .andExpect(jsonPath("$.message").value("El envío todavía no registra movimientos"))
                .andExpect(jsonPath("$.movements", hasSize(0)));
    }

    @Test
    void historialCalculaElEstadoDeCadaMovimientoSegunElAnterior() throws Exception {
        Instant t = REGISTRO.plusSeconds(60);
        Centro cdManizales = centro(40L, "CD Manizales", MANIZALES);
        when(historial.ejecutar(TN)).thenReturn(List.of(
                evento(1L, TN, EventType.RECEIVED_AT_CENTER, cdMedellin, MEDELLIN, t),
                evento(2L, TN, EventType.DISPATCHED, cdMedellin, MEDELLIN, t.plusSeconds(60)),
                evento(3L, TN, EventType.RECEIVED_AT_CENTER, cdManizales, MANIZALES, t.plusSeconds(120))));

        mvc.perform(get("/api/shipments/{tn}/events", TN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovements").value(3))
                .andExpect(jsonPath("$.message").value(nullValue()))
                .andExpect(jsonPath("$.movements[0].id").value(1))
                .andExpect(jsonPath("$.movements[0].resultingStatus").value("AT_DISTRIBUTION_CENTER"))
                .andExpect(jsonPath("$.movements[1].type").value("DISPATCHED"))
                .andExpect(jsonPath("$.movements[1].resultingStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.movements[2].resultingStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.movements[2].cityName").value(MANIZALES.etiqueta()))
                .andExpect(jsonPath("$.movements[2].centerId").value(40));
    }

    @Test
    void historialDeUnEnvioInexistenteResponde404() throws Exception {
        when(historial.ejecutar("NOPE")).thenThrow(new UnknownShipmentException("NOPE"));

        mvc.perform(get("/api/shipments/{tn}/events", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("NOPE")));
    }

    @Test
    void rutaTraduceElItinerarioAlContratoEnIngles() throws Exception {
        when(ruta.ejecutar(TN)).thenReturn(new RutaDelEnvio(TN, "IN_TRANSIT", List.of(
                new Parada(MEDELLIN, TipoParada.ORIGEN, EstadoParada.RECORRIDA),
                new Parada(MANIZALES, TipoParada.ESCALA, EstadoParada.ACTUAL),
                new Parada(BOGOTA, TipoParada.DESTINO, EstadoParada.PENDIENTE)), BOGOTA, 0));

        mvc.perform(get("/api/shipments/{tn}/route", TN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.nextStop.id").value(2))
                .andExpect(jsonPath("$.nextStop.name").value(BOGOTA.etiqueta()))
                .andExpect(jsonPath("$.pendingStopovers").value(0))
                .andExpect(jsonPath("$.stops[0].type").value("ORIGIN"))
                .andExpect(jsonPath("$.stops[0].status").value("VISITED"))
                .andExpect(jsonPath("$.stops[1].type").value("STOPOVER"))
                .andExpect(jsonPath("$.stops[1].status").value("CURRENT"))
                .andExpect(jsonPath("$.stops[2].type").value("DESTINATION"))
                .andExpect(jsonPath("$.stops[2].status").value("PENDING"));
    }

    @Test
    void rutaDeUnEnvioEntregadoNoTieneSiguienteParada() throws Exception {
        when(ruta.ejecutar(TN)).thenReturn(new RutaDelEnvio(TN, "DELIVERED",
                List.of(new Parada(BOGOTA, TipoParada.ORIGEN, EstadoParada.RECORRIDA)), null, 0));

        mvc.perform(get("/api/shipments/{tn}/route", TN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStop").value(nullValue()));
    }

    @Test
    void accionesListaMovimientosCentrosYRuta() throws Exception {
        Centro puntoMedellin = centro(11L, "Punto Medellín", TipoCentro.PUNTO_RECOLECCION, MEDELLIN.id(), true);
        List<com.trackflow.shared.geografia.Ciudad> plan = List.of(MEDELLIN, MANIZALES, BOGOTA);
        EstadoDelEnvio estado = new EstadoDelEnvio(TN, REGISTRO, EventType.RECEIVED_AT_CENTER,
                "AT_DISTRIBUTION_CENTER", REGISTRO.plusSeconds(60), 10L, "CD Medellín", MEDELLIN, MEDELLIN, BOGOTA,
                plan, MANIZALES, FlujoLogistico.siguientesPermitidos(EventType.RECEIVED_AT_CENTER, false));
        when(acciones.ejecutar(TN)).thenReturn(new Acciones(estado, List.of(new Accion(EventType.DISPATCHED,
                CiudadEsperada.ACTUAL, MEDELLIN,
                List.of(new CentroDisponible(cdMedellin, MEDELLIN), new CentroDisponible(puntoMedellin, MEDELLIN)),
                ConsultarAccionesDisponibles.etiquetaDeRuta(plan)))));

        mvc.perform(get("/api/shipments/{tn}/actions", TN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("AT_DISTRIBUTION_CENTER"))
                .andExpect(jsonPath("$.lastMovement").value("RECEIVED_AT_CENTER"))
                .andExpect(jsonPath("$.currentLocation.centerId").value(10))
                .andExpect(jsonPath("$.currentLocation.cityName").value(MEDELLIN.etiqueta()))
                .andExpect(jsonPath("$.origin.id").value(1))
                .andExpect(jsonPath("$.destination.id").value(2))
                .andExpect(jsonPath("$.plannedRoute", hasSize(3)))
                .andExpect(jsonPath("$.actions[0].type").value("DISPATCHED"))
                .andExpect(jsonPath("$.actions[0].label").value("Despachado"))
                .andExpect(jsonPath("$.actions[0].resultingStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.actions[0].expectedCity").value("CURRENT"))
                .andExpect(jsonPath("$.actions[0].centersCity.id").value(1))
                .andExpect(jsonPath("$.actions[0].centers[0].type").value("DISTRIBUTION_CENTER"))
                .andExpect(jsonPath("$.actions[0].centers[1].type").value("PICKUP_POINT"))
                .andExpect(jsonPath("$.actions[0].centers[1].label")
                        .value("Punto Medellín · " + MEDELLIN.etiqueta()))
                .andExpect(jsonPath("$.actions[0].route").value(containsString("→")));
    }

    @Test
    void accionesDeUnEnvioSinMovimientosOfrecenElOrigen() throws Exception {
        EstadoDelEnvio estado = new EstadoDelEnvio(TN, REGISTRO, null, "REGISTERED", null, null, null, MEDELLIN,
                MEDELLIN, BOGOTA, List.of(MEDELLIN, BOGOTA), BOGOTA, FlujoLogistico.siguientesPermitidos(null));
        when(acciones.ejecutar(TN)).thenReturn(new Acciones(estado, List.of(
                new Accion(EventType.RECEIVED_AT_CENTER, CiudadEsperada.ORIGEN, MEDELLIN, List.of(), null))));

        mvc.perform(get("/api/shipments/{tn}/actions", TN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastMovement").value(nullValue()))
                .andExpect(jsonPath("$.currentLocation.centerId").value(nullValue()))
                .andExpect(jsonPath("$.actions[0].expectedCity").value("ORIGIN"))
                .andExpect(jsonPath("$.actions[0].resultingStatus").value("AT_DISTRIBUTION_CENTER"))
                .andExpect(jsonPath("$.actions[0].centers", hasSize(0)));
    }
}
