package com.trackflow.bootstrap.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Verificación de los tokens con el secreto compartido (HMAC-SHA256). Los firma la API de
 * Envíos y acceso; esta API solo los verifica, así que no hay codificador ni contraseñas.
 */
@Configuration
public class JwtConfig {

    private final SecretKey clave;

    public JwtConfig(SecurityProperties propiedades) {
        this.clave = new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * A la firma y el vencimiento se suma la sesión activa (HU-08): un token deja de servir
     * en cuanto se cierra la sesión, pasa el tiempo de inactividad o se desactiva el usuario.
     */
    @Bean
    JwtDecoder jwtDecoder(Sesiones sesiones) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new SesionActivaValidator(sesiones)));
        return decoder;
    }
}
