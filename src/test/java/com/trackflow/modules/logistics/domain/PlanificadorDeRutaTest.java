package com.trackflow.modules.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanificadorDeRutaTest {

    private static final Long MED = 1L;
    private static final Long BOG = 2L;
    private static final Long CAL = 3L;
    private static final Long MAN = 4L;
    private static final Long IBA = 5L;

    @Test
    void escogeElCaminoDeMenosHorasAunqueTengaMasTramos() {
        List<Tramo> red = List.of(
                new Tramo(MED, BOG, 12),
                new Tramo(MED, MAN, 4),
                new Tramo(MAN, BOG, 5));

        Optional<RutaPlaneada> ruta = PlanificadorDeRuta.planificar(red, MED, BOG);

        assertThat(ruta).isPresent();
        assertThat(ruta.get().ciudades()).containsExactly(MED, MAN, BOG);
        assertThat(ruta.get().horas()).isEqualTo(9);
    }

    @Test
    void aIgualDuracionEscogeElDeMenosEscalas() {
        // Dos caminos de 10 horas: directo (1 tramo) o por Manizales y Cali (3 tramos).
        List<Tramo> red = List.of(
                new Tramo(MED, MAN, 3),
                new Tramo(MAN, CAL, 3),
                new Tramo(CAL, BOG, 4),
                new Tramo(MED, BOG, 10));

        RutaPlaneada ruta = PlanificadorDeRuta.planificar(red, MED, BOG).orElseThrow();

        assertThat(ruta.ciudades()).containsExactly(MED, BOG);
        assertThat(ruta.horas()).isEqualTo(10);
    }

    @Test
    void aIgualDuracionPrefiereMenosTramosAunqueSeDescubraDespues() {
        // Por Manizales y Cali (3 tramos, 6 h) se descubre primero, al salir de Cali a las 2 h;
        // por Ibagué (2 tramos, 6 h) aparece después, al salir de Ibagué a las 3 h, y lo reemplaza.
        List<Tramo> red = List.of(
                new Tramo(MED, MAN, 1),
                new Tramo(MAN, CAL, 1),
                new Tramo(CAL, BOG, 4),
                new Tramo(MED, IBA, 3),
                new Tramo(IBA, BOG, 3));

        RutaPlaneada ruta = PlanificadorDeRuta.planificar(red, MED, BOG).orElseThrow();

        assertThat(ruta.ciudades()).containsExactly(MED, IBA, BOG);
        assertThat(ruta.horas()).isEqualTo(6);
    }

    @Test
    void losTramosTienenUnSoloSentido() {
        List<Tramo> red = List.of(new Tramo(BOG, MED, 8));

        assertThat(PlanificadorDeRuta.planificar(red, MED, BOG)).isEmpty();
    }

    @Test
    void sinConexionDevuelveVacio() {
        List<Tramo> red = List.of(new Tramo(MED, MAN, 4), new Tramo(CAL, BOG, 6));

        assertThat(PlanificadorDeRuta.planificar(red, MED, BOG)).isEmpty();
    }

    @Test
    void mismaCiudadEsUnaRutaDeUnaSolaCiudad() {
        RutaPlaneada ruta = PlanificadorDeRuta.planificar(List.of(), BOG, BOG).orElseThrow();

        assertThat(ruta.ciudades()).containsExactly(BOG);
        assertThat(ruta.horas()).isZero();
    }

    @Test
    void sinCiudadDeOrigenODestinoNoHayRuta() {
        List<Tramo> red = List.of(new Tramo(MED, BOG, 10));

        assertThat(PlanificadorDeRuta.planificar(red, null, BOG)).isEmpty();
        assertThat(PlanificadorDeRuta.planificar(red, MED, null)).isEmpty();
    }

    @Test
    void ignoraEntradasViejasDeLaColaCuandoAparecenCaminosMejores() {
        // A Bogotá se llega primero con 20 h y luego se mejora a 6 h por Manizales; la entrada
        // vieja de 20 h sale de la cola antes que Cali (21 h) y debe ignorarse.
        List<Tramo> red = List.of(
                new Tramo(MED, BOG, 20),
                new Tramo(MED, MAN, 2),
                new Tramo(MAN, BOG, 4),
                new Tramo(BOG, CAL, 15));

        RutaPlaneada ruta = PlanificadorDeRuta.planificar(red, MED, CAL).orElseThrow();

        assertThat(ruta.ciudades()).containsExactly(MED, MAN, BOG, CAL);
        assertThat(ruta.horas()).isEqualTo(21);
    }
}
