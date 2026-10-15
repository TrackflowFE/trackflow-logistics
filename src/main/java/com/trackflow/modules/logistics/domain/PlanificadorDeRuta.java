package com.trackflow.modules.logistics.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Calcula la ruta más corta entre dos ciudades sobre los tramos de la red (algoritmo de
 * Dijkstra, ponderado por horas).
 *
 * Es lo que le da sentido geográfico a los hubs intermedios. Antes, un envío Medellín →
 * Bogotá podía "pasar" por Cartagena porque la única regla era que el hub no fuera la
 * ciudad de destino; ahora el hub debe ser la siguiente ciudad de esta ruta.
 *
 * El costo de un camino es la pareja (horas, tramos), comparada en ese orden: a igual
 * tiempo gana el camino con menos tramos, es decir, con menos escalas. Cada escala es
 * una descarga y una recarga del camión, así que entre dos rutas igual de rápidas una
 * transportadora escoge la de menos paradas. Sin este desempate la elección dependía
 * del orden en que el algoritmo descubría las ciudades: en 21 rutas escogía la de más
 * escalas, por ejemplo Barranquilla → Ibagué por el Eje Cafetero (cuatro escalas) en vez
 * de por Bogotá y Girardot (dos), con las mismas horas. Dijkstra sigue siendo correcto
 * con un costo de dos componentes porque los dos solo suman y nunca son negativos.
 *
 * No conoce la base de datos: recibe los tramos y devuelve la ruta, así que la regla
 * se puede razonar y probar sin infraestructura.
 */
public final class PlanificadorDeRuta {

    /** Ciudad por visitar con el costo acumulado para llegar a ella. */
    private record Pendiente(Long ciudad, int horas, int tramos) {
    }

    /** Menos horas primero; con las mismas horas, menos tramos; y si todo empata, la ciudad de id menor. */
    private static final Comparator<Pendiente> MAS_CERCANA_PRIMERO = Comparator
            .comparingInt(Pendiente::horas)
            .thenComparingInt(Pendiente::tramos)
            .thenComparing(Pendiente::ciudad);

    private PlanificadorDeRuta() {
    }

    /**
     * @return la ruta de menor duración (y, entre las de igual duración, la de menos
     *         escalas), o vacío si la red no conecta las dos ciudades
     */
    public static Optional<RutaPlaneada> planificar(Collection<Tramo> tramos, Long desde, Long hasta) {
        if (desde == null || hasta == null) {
            return Optional.empty();
        }
        if (desde.equals(hasta)) {
            return Optional.of(RutaPlaneada.enDestino(desde));
        }

        Map<Long, List<Tramo>> salidas = new HashMap<>();
        for (Tramo tramo : tramos) {
            salidas.computeIfAbsent(tramo.desdeCiudadId(), id -> new ArrayList<>()).add(tramo);
        }

        Map<Long, Pendiente> mejorHasta = new HashMap<>();
        Map<Long, Long> anterior = new HashMap<>();
        PriorityQueue<Pendiente> pendientes = new PriorityQueue<>(MAS_CERCANA_PRIMERO);

        Pendiente inicio = new Pendiente(desde, 0, 0);
        mejorHasta.put(desde, inicio);
        pendientes.add(inicio);

        while (!pendientes.isEmpty()) {
            Pendiente actual = pendientes.poll();

            // Una entrada vieja de la cola: ya se encontró un camino mejor a esta ciudad.
            if (MAS_CERCANA_PRIMERO.compare(actual, mejorHasta.get(actual.ciudad())) > 0) {
                continue;
            }
            if (actual.ciudad().equals(hasta)) {
                return Optional.of(new RutaPlaneada(reconstruir(anterior, desde, hasta), actual.horas()));
            }

            for (Tramo tramo : salidas.getOrDefault(actual.ciudad(), List.of())) {
                Pendiente candidata = new Pendiente(tramo.hastaCiudadId(), actual.horas() + tramo.horas(),
                        actual.tramos() + 1);
                Pendiente conocida = mejorHasta.get(tramo.hastaCiudadId());
                if (conocida == null || esMejor(candidata, conocida)) {
                    mejorHasta.put(tramo.hastaCiudadId(), candidata);
                    anterior.put(tramo.hastaCiudadId(), actual.ciudad());
                    pendientes.add(candidata);
                }
            }
        }

        return Optional.empty();
    }

    /** Menos horas, o las mismas horas con menos tramos. */
    private static boolean esMejor(Pendiente candidata, Pendiente conocida) {
        return candidata.horas() < conocida.horas()
                || (candidata.horas() == conocida.horas() && candidata.tramos() < conocida.tramos());
    }

    private static List<Long> reconstruir(Map<Long, Long> anterior, Long desde, Long hasta) {
        List<Long> ciudades = new ArrayList<>();
        Long ciudad = hasta;
        while (!ciudad.equals(desde)) {
            ciudades.add(ciudad);
            ciudad = anterior.get(ciudad);
        }
        ciudades.add(desde);
        Collections.reverse(ciudades);
        return ciudades;
    }
}
