package com.trackflow.shared.geografia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CatalogoDeCiudadesJpaTest {

    @Mock
    private CiudadJpaRepository jpa;

    @InjectMocks
    private CatalogoDeCiudadesJpa catalogo;

    private static CiudadRegistrada registrada(long id, String nombre, String departamento) {
        CiudadRegistrada ciudad = new CiudadRegistrada();
        ReflectionTestUtils.setField(ciudad, "id", id);
        ReflectionTestUtils.setField(ciudad, "name", nombre);
        ReflectionTestUtils.setField(ciudad, "department", departamento);
        return ciudad;
    }

    @Test
    void porIdConvierteAlModeloDeDominio() {
        when(jpa.findById(1L)).thenReturn(Optional.of(registrada(1, "MEDELLÍN", "ANTIOQUIA")));

        Optional<Ciudad> ciudad = catalogo.porId(1L);

        assertThat(ciudad).contains(new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA"));
        assertThat(ciudad.get().etiqueta()).isEqualTo("MEDELLÍN - ANTIOQUIA");
    }

    @Test
    void porIdNuloNoConsultaLaBase() {
        assertThat(catalogo.porId(null)).isEmpty();
        verifyNoInteractions(jpa);
    }

    @Test
    void exigirFallaConLaCiudadQueNoEsta() {
        when(jpa.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogo.exigir(999L))
                .isInstanceOf(CiudadDesconocidaException.class)
                .hasMessageContaining("999")
                .satisfies(e -> assertThat(((CiudadDesconocidaException) e).getId()).isEqualTo(999L));
    }

    @Test
    void buscarRecortaElTextoYAplicaElLimite() {
        when(jpa.buscar(eq("bogo"), any(Limit.class))).thenReturn(List.of(registrada(2, "BOGOTÁ D.C.", "BOGOTÁ")));

        List<Ciudad> encontradas = catalogo.buscar("  bogo ", 10);

        assertThat(encontradas).containsExactly(new Ciudad(2L, "BOGOTÁ D.C.", "BOGOTÁ"));
        ArgumentCaptor<Limit> limite = ArgumentCaptor.forClass(Limit.class);
        verify(jpa).buscar(eq("bogo"), limite.capture());
        assertThat(limite.getValue().max()).isEqualTo(10);
    }

    @Test
    void buscarSinTextoNoConsultaLaBase() {
        assertThat(catalogo.buscar(null, 10)).isEmpty();
        assertThat(catalogo.buscar("  ", 10)).isEmpty();
        verifyNoInteractions(jpa);
    }
}
