package com.trackflow.modules.logistics.infrastructure;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.domain.Centro;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class CatalogoDeCentrosJpaTest {

    @Mock
    private CentroJpaRepository jpa;

    @InjectMocks
    private CatalogoDeCentrosJpa catalogo;

    @Test
    void porIdNuloNoConsultaLaBase() {
        assertThat(catalogo.porId(null)).isEmpty();
        verifyNoInteractions(jpa);
    }

    @Test
    void porIdDelegaEnElRepositorio() {
        Centro centro = centro(10L, "CD Medellín", MEDELLIN);
        when(jpa.findById(10L)).thenReturn(Optional.of(centro));

        assertThat(catalogo.porId(10L)).containsSame(centro);
    }

    @Test
    void buscarRecortaElTextoYAplicaElLimite() {
        List<Centro> encontrados = List.of(centro(10L, "CD Medellín", MEDELLIN));
        when(jpa.buscar(eq("med"), eq(1L), any(Limit.class))).thenReturn(encontrados);

        List<Centro> resultado = catalogo.buscar("  med ", 1L, 20);

        assertThat(resultado).isEqualTo(encontrados);
        ArgumentCaptor<Limit> limite = ArgumentCaptor.forClass(Limit.class);
        verify(jpa).buscar(eq("med"), eq(1L), limite.capture());
        assertThat(limite.getValue().max()).isEqualTo(20);
    }

    @Test
    void unTextoEnBlancoSeBuscaComoSinTexto() {
        when(jpa.buscar(isNull(), isNull(), any(Limit.class))).thenReturn(List.of());

        assertThat(catalogo.buscar("   ", null, 5)).isEmpty();
        assertThat(catalogo.buscar(null, null, 5)).isEmpty();
    }
}
