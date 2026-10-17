package com.trackflow.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class SesionActivaValidatorTest {

    private final Sesiones sesiones = mock(Sesiones.class);
    private final SesionActivaValidator validador = new SesionActivaValidator(sesiones);

    private static Jwt token(String jti) {
        Instant ahora = Instant.parse("2026-09-02T12:00:00Z");
        return Jwt.withTokenValue("token").header("alg", "HS256").jti(jti).subject("operador")
                .issuedAt(ahora).expiresAt(ahora.plusSeconds(3600)).build();
    }

    @Test
    void unaSesionVigenteValida() {
        when(sesiones.validarYRenovar("jti-1")).thenReturn(true);

        OAuth2TokenValidatorResult resultado = validador.validate(token("jti-1"));

        assertThat(resultado.hasErrors()).isFalse();
    }

    @Test
    void unaSesionTerminadaEsUnTokenInvalido() {
        when(sesiones.validarYRenovar("jti-2")).thenReturn(false);

        OAuth2TokenValidatorResult resultado = validador.validate(token("jti-2"));

        assertThat(resultado.hasErrors()).isTrue();
        assertThat(resultado.getErrors()).singleElement().satisfies(error -> {
            assertThat(error.getErrorCode()).isEqualTo("invalid_token");
            assertThat(error.getDescription()).contains("inicie sesión de nuevo");
        });
    }
}
