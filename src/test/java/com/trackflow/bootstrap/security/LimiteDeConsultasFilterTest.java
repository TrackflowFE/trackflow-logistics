package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.shared.seguridad.IpDelCliente;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * El historial y el itinerario son públicos porque el número de seguimiento es la
 * credencial del cliente. Sin límite, ese diseño permite adivinar números a fuerza bruta
 * y descargar historiales en masa, que es lo que este filtro corta.
 */
class LimiteDeConsultasFilterTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T12:00:00Z");
    private static final String ORIGENES = "https://cliente.example";

    private LimiteDeConsultasFilter filtro(int limite, long segundos, Clock reloj) {
        return new LimiteDeConsultasFilter(limite, segundos, reloj, new IpDelCliente(0, ""), ORIGENES);
    }

    private MockHttpServletRequest peticion(String ruta, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.setServletPath(ruta);
        request.setRemoteAddr(ip);
        request.addHeader("Origin", ORIGENES);
        return request;
    }

    private MockHttpServletResponse pasar(LimiteDeConsultasFilter filtro, String ruta, String ip) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(peticion(ruta, ip), response, (req, res) -> { /* la API respondería aquí */ });
        return response;
    }

    @Test
    void cortaElSondeoMasivoDeNumerosDeSeguimiento() throws Exception {
        LimiteDeConsultasFilter filtro = filtro(3, 60, Clock.fixed(AHORA, ZoneOffset.UTC));

        // Números distintos: quien adivina a fuerza bruta nunca repite el mismo.
        assertEquals(200, pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7").getStatus());
        assertEquals(200, pasar(filtro, "/api/shipments/TF0000000002/events", "203.0.113.7").getStatus());
        assertEquals(200, pasar(filtro, "/api/shipments/TF0000000003/events", "203.0.113.7").getStatus());
        assertEquals(429, pasar(filtro, "/api/shipments/TF0000000004/events", "203.0.113.7").getStatus());
    }

    @Test
    void elItinerarioComparteLaMismaCuotaQueElHistorial() throws Exception {
        LimiteDeConsultasFilter filtro = filtro(2, 60, Clock.fixed(AHORA, ZoneOffset.UTC));

        assertEquals(200, pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7").getStatus());
        assertEquals(200, pasar(filtro, "/api/shipments/TF0000000001/route", "203.0.113.7").getStatus());
        assertEquals(429, pasar(filtro, "/api/shipments/TF0000000001/route", "203.0.113.7").getStatus());
    }

    @Test
    void el429LlegaConCorsParaQueElNavegadorPuedaLeerlo() throws Exception {
        LimiteDeConsultasFilter filtro = filtro(1, 45, Clock.fixed(AHORA, ZoneOffset.UTC));
        pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7");

        MockHttpServletResponse response = pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7");

        assertEquals(429, response.getStatus());
        // Sin esta cabecera el navegador muestra un error de CORS y el cliente nunca
        // ve el mensaje de "espere unos segundos".
        assertEquals(ORIGENES, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("45", response.getHeader("Retry-After"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
        // El charset va explicito: sin el, el contenedor usa ISO-8859-1.
        assertEquals("application/problem+json;charset=UTF-8", response.getContentType());
    }

    @Test
    void laCuotaEsPorIpAsiQueUnClienteNoPagaPorOtro() throws Exception {
        LimiteDeConsultasFilter filtro = filtro(1, 60, Clock.fixed(AHORA, ZoneOffset.UTC));
        pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7");

        assertEquals(429, pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7").getStatus());
        assertEquals(200, pasar(filtro, "/api/shipments/TF0000000001/events", "198.51.100.4").getStatus());
    }

    @Test
    void laVentanaSeLiberaCuandoPasaElTiempo() throws Exception {
        RelojAjustable reloj = new RelojAjustable(AHORA);
        LimiteDeConsultasFilter filtro = filtro(1, 60, reloj);

        pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7");
        assertEquals(429, pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7").getStatus());

        reloj.avanzar(Duration.ofSeconds(61));
        assertEquals(200, pasar(filtro, "/api/shipments/TF0000000001/events", "203.0.113.7").getStatus());
    }

    @Test
    void noSeMeteConLoQueYaExigeToken() {
        LimiteDeConsultasFilter filtro = filtro(1, 60, Clock.fixed(AHORA, ZoneOffset.UTC));

        // Estas rutas piden rol de operador: las protege la cadena de seguridad.
        assertTrue(filtro.shouldNotFilter(peticion("/api/centers", "203.0.113.7")));
        assertTrue(filtro.shouldNotFilter(peticion("/api/shipments/TF0000000001/actions", "203.0.113.7")));

        MockHttpServletRequest escritura = peticion("/api/shipments/TF0000000001/events", "203.0.113.7");
        escritura.setMethod("POST");
        assertTrue(filtro.shouldNotFilter(escritura));
    }

    @Test
    void elPreflightNoConsumeCuota() {
        LimiteDeConsultasFilter filtro = filtro(1, 60, Clock.fixed(AHORA, ZoneOffset.UTC));

        MockHttpServletRequest preflight = peticion("/api/shipments/TF0000000001/events", "203.0.113.7");
        preflight.setMethod("OPTIONS");
        assertTrue(filtro.shouldNotFilter(preflight));
    }

    @Test
    void rechazaUnaConfiguracionQueDejariaElLimiteSinSentido() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

        assertThrows(IllegalArgumentException.class, () -> filtro(0, 60, reloj));
        assertThrows(IllegalArgumentException.class, () -> filtro(10, 0, reloj));
        assertThrows(IllegalArgumentException.class, () -> filtro(10, 86401, reloj));
    }

    /** Reloj que se puede adelantar, para comprobar que la ventana caduca. */
    private static final class RelojAjustable extends Clock {
        private Instant instante;

        private RelojAjustable(Instant instante) {
            this.instante = instante;
        }

        void avanzar(Duration cuanto) {
            instante = instante.plus(cuanto);
        }

        @Override
        public Instant instant() {
            return instante;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }
    }
}
