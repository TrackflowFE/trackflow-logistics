package com.trackflow.bootstrap.security;

import com.trackflow.shared.seguridad.IpDelCliente;
import com.trackflow.shared.seguridad.VentanasDeIntentos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite de consultas por IP al historial y al itinerario públicos.
 *
 * Esas dos rutas son públicas a propósito: el número de seguimiento es la credencial del
 * cliente, que no tiene cuenta. Eso deja la puerta abierta a dos abusos que la
 * autenticación no cubre, porque aquí no hay a quién autenticar: adivinar números a
 * fuerza bruta, y descargar el historial de envíos conocidos de forma masiva.
 *
 * Cada petición cuenta y no se libera: el límite es de consultas, no de fallos.
 *
 * La cuenta es por IP y vive en memoria, así que cada instancia lleva la suya;
 * centralizarla es trabajo del API Gateway, no de este filtro.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class LimiteDeConsultasFilter extends OncePerRequestFilter {

    /** /api/shipments/{numero}/events y /route, que son las dos rutas sin token. */
    private static final Pattern RUTAS = Pattern.compile("^/api/shipments/[^/]+/(events|route)$");

    private static final int MAX_CLAVES = 10000;

    private final VentanasDeIntentos ventanas;
    private final IpDelCliente ipDelCliente;
    private final int limite;
    private final long segundos;
    private final CorsConfiguration cors;

    public LimiteDeConsultasFilter(@Value("${trackflow.consultas.limite.ip:60}") int limite,
            @Value("${trackflow.consultas.limite.ventana-segundos:60}") long segundos,
            Clock clock, IpDelCliente ipDelCliente,
            @Value("${trackflow.cors.allowed-origins:http://localhost:5173}") String origenes) {
        if (limite < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException(
                    "El límite de consultas debe ser positivo y la ventana de hasta 86400 segundos");
        }
        this.limite = limite;
        this.segundos = segundos;
        this.ventanas = new VentanasDeIntentos(MAX_CLAVES, segundos * 1000, clock);
        this.ipDelCliente = ipDelCliente;
        // Este filtro corre antes de la configuración CORS de Spring MVC, así que el 429
        // saldría sin Access-Control-Allow-Origin: el navegador mostraría un error de CORS
        // y el cliente nunca vería el mensaje de "espere unos segundos".
        this.cors = new CorsConfiguration();
        this.cors.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim)
                .filter(origen -> !origen.isEmpty()).toList());
        this.cors.setAllowedMethods(List.of("GET", "OPTIONS"));
        this.cors.setAllowedHeaders(List.of("*"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // El preflight no trae credenciales ni cuesta trabajo: si contara, el navegador
        // gastaría la cuota del cliente antes de su primera consulta real.
        return !"GET".equals(request.getMethod()) || !RUTAS.matcher(request.getServletPath()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = ipDelCliente.obtener(request);
        if (ventanas.reservar(ip, limite) == null) {
            if (!new DefaultCorsProcessor().processRequest(cors, request, response)) {
                return;
            }
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(segundos));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Demasiadas consultas","status":429,\
                    "detail":"Espere unos segundos antes de volver a consultar."}""");
            return;
        }
        chain.doFilter(request, response);
    }
}
