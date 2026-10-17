package com.trackflow.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SesionesTest {

    private static final Instant AHORA = Instant.parse("2026-09-02T12:00:00Z");

    @Mock
    private SesionRepository repositorio;

    @Mock
    private UsuarioRepository usuarios;

    private Sesiones sesiones;

    @BeforeEach
    void setUp() {
        sesiones = new Sesiones(repositorio, usuarios, new SecurityProperties(true, "s".repeat(32), 30),
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Sesion sesionCreadaHace(Duration hace) {
        Sesion sesion = new Sesion("jti-1", "operador", AHORA.minus(hace), AHORA.plus(Duration.ofHours(8)));
        when(repositorio.findById("jti-1")).thenReturn(Optional.of(sesion));
        return sesion;
    }

    private void usuario(boolean activo) {
        when(usuarios.findById("operador"))
                .thenReturn(Optional.of(new Usuario("operador", "hash", "OPERADOR", activo, AHORA)));
    }

    @Test
    void unaSesionVigenteRenuevaSuActividad() {
        Sesion sesion = sesionCreadaHace(Duration.ofMinutes(10));
        usuario(true);

        assertThat(sesiones.validarYRenovar("jti-1")).isTrue();
        assertThat(ReflectionTestUtils.getField(sesion, "lastActivityAt")).isEqualTo(AHORA);
        assertThat(sesion.abierta()).isTrue();
    }

    @Test
    void conActividadRecienteNoReescribe() {
        Sesion sesion = sesionCreadaHace(Duration.ofSeconds(30));
        usuario(true);

        assertThat(sesiones.validarYRenovar("jti-1")).isTrue();
        assertThat(ReflectionTestUtils.getField(sesion, "lastActivityAt")).isEqualTo(AHORA.minusSeconds(30));
    }

    @Test
    void unaSesionInactivaSeCierraYSeRechaza() {
        Sesion sesion = sesionCreadaHace(Duration.ofMinutes(31));
        usuario(true);

        assertThat(sesiones.validarYRenovar("jti-1")).isFalse();
        assertThat(sesion.abierta()).isFalse();
        assertThat(ReflectionTestUtils.getField(sesion, "closedAt")).isEqualTo(AHORA);
    }

    @Test
    void unUsuarioDesactivadoCierraLaSesion() {
        Sesion sesion = sesionCreadaHace(Duration.ofMinutes(1));
        usuario(false);

        assertThat(sesiones.validarYRenovar("jti-1")).isFalse();
        assertThat(sesion.abierta()).isFalse();
    }

    @Test
    void unUsuarioQueYaNoExisteCierraLaSesion() {
        Sesion sesion = sesionCreadaHace(Duration.ofMinutes(1));
        when(usuarios.findById("operador")).thenReturn(Optional.empty());

        assertThat(sesiones.validarYRenovar("jti-1")).isFalse();
        assertThat(sesion.abierta()).isFalse();
    }

    @Test
    void unaSesionYaCerradaSeRechazaSinConsultarElUsuario() {
        Sesion sesion = sesionCreadaHace(Duration.ofMinutes(1));
        sesion.cerrar(AHORA.minusSeconds(5));

        assertThat(sesiones.validarYRenovar("jti-1")).isFalse();
        verifyNoInteractions(usuarios);
        // Cerrar de nuevo no cambia la fecha del primer cierre.
        sesion.cerrar(AHORA);
        assertThat(ReflectionTestUtils.getField(sesion, "closedAt")).isEqualTo(AHORA.minusSeconds(5));
    }

    @Test
    void unaSesionDesconocidaOSinJtiSeRechaza() {
        when(repositorio.findById("otro")).thenReturn(Optional.empty());

        assertThat(sesiones.validarYRenovar("otro")).isFalse();
        assertThat(sesiones.validarYRenovar(null)).isFalse();
    }
}
