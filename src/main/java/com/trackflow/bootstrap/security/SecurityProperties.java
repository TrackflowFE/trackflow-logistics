package com.trackflow.bootstrap.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuración de seguridad tomada del entorno.
 *
 * Esta API no emite tokens: los emite la API de Envíos y acceso al iniciar sesión, y aquí
 * solo se verifican con el mismo secreto. Por eso el secreto es obligatorio cuando la
 * protección está activa: sin él no habría forma de aceptar un token legítimo.
 */
@Component
public class SecurityProperties {

    public static final String ROL_OPERADOR = "OPERADOR";
    public static final String ROL_ADMIN = "ADMIN";

    private final boolean activa;
    private final String secreto;
    private final Duration inactividad;

    public SecurityProperties(
            @Value("${trackflow.security.enabled:true}") boolean activa,
            @Value("${trackflow.security.jwt-secret:}") String secreto,
            @Value("${trackflow.security.inactividad-minutos:30}") long inactividadMinutos) {
        this.activa = activa;
        this.secreto = secreto;
        this.inactividad = Duration.ofMinutes(inactividadMinutos);
    }

    public boolean proteccionActiva() {
        return activa;
    }

    /**
     * El secreto compartido con las otras dos APIs. Con la protección activa debe tener al
     * menos 32 caracteres (HMAC-SHA256); si falta, el arranque falla en vez de aceptar o
     * rechazar tokens a ciegas.
     */
    public String secreto() {
        if (secreto != null && secreto.length() >= 32) {
            return secreto;
        }
        if (activa) {
            throw new IllegalStateException("Defina TRACKFLOW_JWT_SECRET (mínimo 32 caracteres), "
                    + "el mismo valor que usa la API de Envíos y acceso para firmar los tokens");
        }
        // Sin protección no se valida ningún token; basta un valor de relleno para el decodificador.
        return "x".repeat(32);
    }

    /** Tiempo sin actividad tras el que la sesión se cierra (HU-08). */
    public Duration inactividad() {
        return inactividad;
    }
}
