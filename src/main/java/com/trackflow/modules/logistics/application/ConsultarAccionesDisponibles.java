package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Qué puede hacer el operador con este envío, ahora mismo.
 *
 * Responde con los movimientos admisibles y, para cada uno, los centros concretos
 * entre los que puede escoger y la ruta que le corresponde. La interfaz no tiene que
 * decidir nada ni conocer el recorrido: pinta lo que le llega. Como las opciones
 * salen de las mismas reglas que después validan el registro
 * ({@link FlujoLogistico}), es imposible que se ofrezca algo que vaya a rechazarse.
 */
@Service
public class ConsultarAccionesDisponibles {

    private static final int LIMITE_CENTROS = 20;

    /** Un centro con su ciudad ya resuelta, listo para mostrarse. */
    public record CentroDisponible(Centro centro, Ciudad ciudad) {
    }

    /**
     * @param ciudadDeLosCentros ciudad a la que pertenecen los centros ofrecidos
     * @param ruta trayecto que representa el movimiento, solo para el despacho
     */
    public record Accion(EventType tipo, CiudadEsperada ciudadEsperada, Ciudad ciudadDeLosCentros,
            List<CentroDisponible> centros, String ruta) {
    }

    public record Acciones(EstadoDelEnvio estado, List<Accion> acciones) {
    }

    private final ConsultarEstadoDelEnvio estadoDelEnvio;
    private final CatalogoDeCentros centros;

    public ConsultarAccionesDisponibles(ConsultarEstadoDelEnvio estadoDelEnvio, CatalogoDeCentros centros) {
        this.estadoDelEnvio = estadoDelEnvio;
        this.centros = centros;
    }

    public Acciones ejecutar(String trackingNumber) {
        EstadoDelEnvio estado = estadoDelEnvio.ejecutar(trackingNumber);

        List<Accion> acciones = estado.siguientesPermitidos().stream()
                .map(tipo -> construir(tipo, estado))
                .toList();

        return new Acciones(estado, acciones);
    }

    private Accion construir(EventType tipo, EstadoDelEnvio estado) {
        CiudadEsperada esperada = FlujoLogistico.ciudadEsperada(tipo, estado.ultimoTipo());
        Ciudad ciudad = estado.ciudadQueExige(esperada);

        return new Accion(tipo, esperada, ciudad, centrosPara(ciudad), rutaPara(tipo, estado));
    }

    /**
     * Toda regla señala una ciudad concreta, también la de un hub (la siguiente de la
     * ruta), así que basta con filtrar por ella. Antes, en el paso por un hub se
     * ofrecían los primeros centros del catálogo en orden alfabético, de cualquier
     * ciudad, y la opción correcta podía ni aparecer.
     */
    private List<CentroDisponible> centrosPara(Ciudad ciudad) {
        return centros.buscar(null, ciudad.id(), LIMITE_CENTROS).stream()
                .map(centro -> new CentroDisponible(centro, ciudad))
                .toList();
    }

    /**
     * El despacho es el único movimiento que representa un trayecto: la ruta planeada
     * desde donde está el paquete hasta el destino, con las paradas intermedias. El
     * operador no la escribe.
     */
    private String rutaPara(EventType tipo, EstadoDelEnvio estado) {
        if (tipo != EventType.DISPATCHED) {
            return null;
        }
        return etiquetaDeRuta(estado.rutaPlaneada());
    }

    public static String etiquetaDeRuta(List<Ciudad> ruta) {
        return ruta.stream().map(Ciudad::etiqueta).collect(Collectors.joining(" → "));
    }
}
