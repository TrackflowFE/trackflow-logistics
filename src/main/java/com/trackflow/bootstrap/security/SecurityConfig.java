package com.trackflow.bootstrap.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * La escritura exige un token de operador emitido por la API de Envíos y acceso; el
 * historial y el itinerario son públicos, porque el número de seguimiento es la
 * credencial del cliente. La protección se desactiva solo con TRACKFLOW_SECURITY_ENABLED=false.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final SecurityProperties propiedades;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) {
        boolean protegido = propiedades.proteccionActiva();

        http
                // Sin esto, el filtro de seguridad rechaza las peticiones preflight antes de
                // que se aplique la configuración CORS de Spring MVC (ver CorsConfig).
                .cors(Customizer.withDefaults())
                // CSRF explota que el navegador adjunta solo las cookies de sesión. Aquí no
                // hay cookies: la sesión es STATELESS y el JWT viaja en la cabecera
                // Authorization, que un sitio ajeno no puede añadir. Por eso se desactiva.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 - API sin estado, JWT en cabecera, sin cookies
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // El OPTIONS de precomprobación nunca lleva token, por diseño del propio
                    // CORS: si una regla de rol lo alcanza (como pasaba con /api/admin/**, que
                    // no distinguía el método), el navegador ve un 401 en la precomprobación y
                    // ni siquiera intenta la petición real. Dejarlo pasar aquí no abre nada: la
                    // petición real —GET, POST— sigue evaluándose por las reglas de abajo.
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    // Lo que ve el cliente final: el número de seguimiento es su credencial.
                    auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/events", "/api/shipments/*/route")
                            .permitAll();
                    // /error es a donde Spring reenvía las respuestas de error: si se
                    // cerrara, un 404 llegaría al cliente como 401.
                    auth.requestMatchers("/", "/error", "/actuator/health/**", "/swagger-ui.html",
                            "/swagger-ui/**", "/v3/api-docs/**").permitAll();

                    if (protegido) {
                        // Las operaciones de mantenimiento no las hace quien registra paquetes.
                        auth.requestMatchers("/api/admin/**").hasRole(SecurityProperties.ROL_ADMIN);
                        auth.requestMatchers(HttpMethod.POST, "/api/**")
                                .hasRole(SecurityProperties.ROL_OPERADOR);
                        // El estado interno del envío y la red de centros son de la
                        // operación, no del cliente: los consulta quien registra movimientos.
                        auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/actions", "/api/centers",
                                "/api/centers/**").hasRole(SecurityProperties.ROL_OPERADOR);
                        // Lo que no esté listado arriba queda cerrado: un endpoint nuevo no
                        // puede quedar público por olvido.
                        auth.anyRequest().denyAll();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRoles())));

        if (protegido) {
            log.info("Seguridad activa: la escritura requiere un token de operador de la API de Envíos y acceso");
        } else {
            log.warn("TRACKFLOW_SECURITY_ENABLED=false: los endpoints de escritura están abiertos. "
                    + "Nunca en un entorno accesible desde internet.");
        }

        return http.build();
    }

    /** Traduce el claim "roles" del token a las autoridades que espera Spring Security. */
    private JwtAuthenticationConverter conversorDeRoles() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
