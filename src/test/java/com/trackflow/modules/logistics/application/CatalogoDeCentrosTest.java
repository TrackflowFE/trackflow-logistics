package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CentroInvalidoException;
import com.trackflow.modules.logistics.domain.TipoCentro;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Prueba la regla por defecto {@link CatalogoDeCentros#exigir} sobre un catálogo en memoria. */
class CatalogoDeCentrosTest {

    private final Centro activo = centro(10L, "CD Medellín", MEDELLIN);
    private final Centro inactivo = centro(11L, "CD Cerrado", TipoCentro.PUNTO_RECOLECCION, MEDELLIN.id(), false);

    private final CatalogoDeCentros catalogo = new CatalogoDeCentros() {
        private final Map<Long, Centro> porId = Map.of(10L, activo, 11L, inactivo);

        @Override
        public Optional<Centro> porId(Long id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public List<Centro> buscar(String texto, Long ciudadId, int limite) {
            return List.of();
        }
    };

    @Test
    void exigirDevuelveUnCentroActivo() {
        assertThat(catalogo.exigir(10L)).isSameAs(activo);
    }

    @Test
    void exigirRechazaUnCentroInexistente() {
        assertThatThrownBy(() -> catalogo.exigir(99L))
                .isInstanceOf(CentroInvalidoException.class)
                .hasMessageContaining("El centro 99 no existe");
    }

    @Test
    void exigirRechazaUnCentroInactivo() {
        assertThatThrownBy(() -> catalogo.exigir(11L))
                .isInstanceOf(CentroInvalidoException.class)
                .hasMessageContaining("CD Cerrado")
                .hasMessageContaining("inactivo");
    }
}
