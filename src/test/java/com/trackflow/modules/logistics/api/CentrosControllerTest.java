package com.trackflow.modules.logistics.api;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.modules.logistics.application.CatalogoDeCentros;
import com.trackflow.modules.logistics.domain.TipoCentro;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CentrosControllerTest {

    private CatalogoDeCentros centros;
    private CatalogoDeCiudades ciudades;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        centros = mock(CatalogoDeCentros.class);
        ciudades = mock(CatalogoDeCiudades.class);
        when(ciudades.exigir(MEDELLIN.id())).thenReturn(MEDELLIN);
        when(ciudades.exigir(BOGOTA.id())).thenReturn(BOGOTA);
        mvc = MockMvcBuilders.standaloneSetup(new CentrosController(centros, ciudades))
                .setControllerAdvice(new LogisticsExceptionHandler())
                .build();
    }

    @Test
    void buscarDevuelveLosCentrosConSuCiudadYTipoEnIngles() throws Exception {
        when(centros.buscar("med", 1L, 20)).thenReturn(List.of(
                centro(10L, "CD Medellín", MEDELLIN),
                centro(12L, "Oficina El Poblado", TipoCentro.OFICINA, MEDELLIN.id(), true)));

        mvc.perform(get("/api/centers").param("q", "med").param("cityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("CD Medellín"))
                .andExpect(jsonPath("$[0].type").value("DISTRIBUTION_CENTER"))
                .andExpect(jsonPath("$[0].cityId").value(1))
                .andExpect(jsonPath("$[0].cityName").value(MEDELLIN.etiqueta()))
                .andExpect(jsonPath("$[0].label").value("CD Medellín · " + MEDELLIN.etiqueta()))
                .andExpect(jsonPath("$[1].type").value("OFFICE"));
    }

    @Test
    void buscarSinFiltrosPideElLimiteDeVeinte() throws Exception {
        when(centros.buscar(null, null, 20)).thenReturn(List.of());

        mvc.perform(get("/api/centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        verify(centros).buscar(null, null, 20);
    }

    @Test
    void detalleDevuelveElCentro() throws Exception {
        when(centros.porId(20L)).thenReturn(Optional.of(centro(20L, "CD Bogotá", BOGOTA)));

        mvc.perform(get("/api/centers/{id}", 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CD Bogotá"))
                .andExpect(jsonPath("$.cityName").value(BOGOTA.etiqueta()));
    }

    @Test
    void detalleDeUnCentroInexistenteResponde404() throws Exception {
        when(centros.porId(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/centers/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un centro con id 99"));
    }
}
