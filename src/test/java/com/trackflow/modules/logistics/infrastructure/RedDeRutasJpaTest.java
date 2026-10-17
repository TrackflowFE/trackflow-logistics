package com.trackflow.modules.logistics.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.domain.Tramo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedDeRutasJpaTest {

    @Mock
    private TramoDeRutaJpaRepository jpa;

    @InjectMocks
    private RedDeRutasJpa red;

    private static TramoDeRuta tramo(long desde, long hasta, int horas) {
        TramoDeRuta tramo = new TramoDeRuta();
        ReflectionTestUtils.setField(tramo, "fromCityId", desde);
        ReflectionTestUtils.setField(tramo, "toCityId", hasta);
        ReflectionTestUtils.setField(tramo, "hours", horas);
        ReflectionTestUtils.setField(tramo, "active", true);
        return tramo;
    }

    @Test
    void convierteLosTramosDeLaBaseEnTramosDelDominio() {
        when(jpa.activosEntreCiudadesConCobertura()).thenReturn(List.of(tramo(1, 4, 4), tramo(4, 2, 5)));

        List<Tramo> tramos = red.tramosActivos();

        assertThat(tramos).containsExactly(new Tramo(1L, 4L, 4), new Tramo(4L, 2L, 5));
    }

    @Test
    void sinTramosLaRedEstaVacia() {
        when(jpa.activosEntreCiudadesConCobertura()).thenReturn(List.of());

        assertThat(red.tramosActivos()).isEmpty();
    }
}
