package com.trackflow.modules.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RutaPlaneadaTest {

    @Test
    void laSiguienteParadaEsLaSegundaCiudad() {
        RutaPlaneada ruta = new RutaPlaneada(List.of(1L, 4L, 2L), 9);

        assertThat(ruta.siguienteParada()).isEqualTo(4L);
        assertThat(ruta.destino()).isEqualTo(2L);
    }

    @Test
    void enDestinoLaSiguienteParadaEsElDestinoMismo() {
        RutaPlaneada ruta = RutaPlaneada.enDestino(2L);

        assertThat(ruta.siguienteParada()).isEqualTo(2L);
        assertThat(ruta.destino()).isEqualTo(2L);
        assertThat(ruta.horas()).isZero();
    }

    @Test
    void unaRutaSinCiudadesEsInvalida() {
        assertThatThrownBy(() -> new RutaPlaneada(List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una ciudad");
        assertThatThrownBy(() -> new RutaPlaneada(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copiaLaListaParaQueNoCambieDesdeFuera() {
        List<Long> ciudades = new ArrayList<>(List.of(1L, 2L));
        RutaPlaneada ruta = new RutaPlaneada(ciudades, 10);

        ciudades.add(3L);

        assertThat(ruta.ciudades()).containsExactly(1L, 2L);
        assertThatThrownBy(() -> ruta.ciudades().add(5L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
