package com.trackflow.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

    private static final String SECRETO = "s".repeat(32);

    @Test
    void conProteccionYSecretoValidoLoDevuelve() {
        SecurityProperties propiedades = new SecurityProperties(true, SECRETO, 30);

        assertThat(propiedades.proteccionActiva()).isTrue();
        assertThat(propiedades.secreto()).isEqualTo(SECRETO);
        assertThat(propiedades.inactividad()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void conProteccionYSecretoCortoFallaElArranque() {
        SecurityProperties propiedades = new SecurityProperties(true, "corto", 30);

        assertThatThrownBy(propiedades::secreto)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TRACKFLOW_JWT_SECRET");
    }

    @Test
    void conProteccionYSinSecretoFallaElArranque() {
        assertThatThrownBy(() -> new SecurityProperties(true, null, 30).secreto())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinProteccionUsaUnSecretoDeRelleno() {
        SecurityProperties propiedades = new SecurityProperties(false, "", 5);

        assertThat(propiedades.proteccionActiva()).isFalse();
        assertThat(propiedades.secreto()).hasSize(32);
        assertThat(propiedades.inactividad()).isEqualTo(Duration.ofMinutes(5));
    }
}
