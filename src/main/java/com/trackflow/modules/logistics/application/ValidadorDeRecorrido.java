package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CentroFueraDeCiudadException;
import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.MovimientoAntesDelRegistroException;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Las reglas del recorrido, en un solo lugar: el movimiento debe caber en el orden del
 * flujo y en la ruta, no puede retroceder en el tiempo ni ser anterior al registro del
 * envío, y el centro debe estar activo y en la ciudad que el recorrido exige.
 *
 * Lo usan las dos puntas de la ingesta. La admisión las aplica para responder con su
 * código HTTP antes de encolar; el registro las vuelve a aplicar al consumir la cola,
 * porque es el único que escribe. Sin esa segunda pasada, dos peticiones simultáneas
 * pasaban la admisión y encolaban dos despachos, y un productor que publicaba directo
 * en la cola se saltaba todas las reglas.
 */
@Service
public class ValidadorDeRecorrido {

    /** El centro donde ocurrió el movimiento, con su ciudad, ya validados. */
    public record LugarValidado(Centro centro, Ciudad ciudad) {
    }

    private final CatalogoDeCentros centros;
    private final CatalogoDeCiudades ciudades;

    public ValidadorDeRecorrido(CatalogoDeCentros centros, CatalogoDeCiudades ciudades) {
        this.centros = centros;
        this.ciudades = ciudades;
    }

    public LugarValidado validar(EstadoDelEnvio estado, EventType tipo, Long centroId, Instant ocurridoEn) {
        validarTransicion(estado, tipo, ocurridoEn);

        Centro centro = centros.exigir(centroId);
        Ciudad ciudadCentro = ciudades.exigir(centro.getCityId());
        validarCiudadDelCentro(estado, tipo, centro, ciudadCentro);

        return new LugarValidado(centro, ciudadCentro);
    }

    /**
     * En el orden correcto, acorde con la ruta (tras un despacho, llegada si la
     * siguiente parada es el destino, hub si no) y sin retroceder en el tiempo.
     *
     * Tampoco antes del registro: un paquete no puede recibirse en un centro antes de
     * que el envío exista. Sin esta regla solo se comparaba contra el último
     * movimiento, así que el primero podía tener cualquier fecha pasada.
     */
    private static void validarTransicion(EstadoDelEnvio estado, EventType tipo, Instant ocurridoEn) {
        if (ocurridoEn.isBefore(estado.registradoEn())) {
            throw new MovimientoAntesDelRegistroException(estado.trackingNumber(), ocurridoEn,
                    estado.registradoEn());
        }

        if (!estado.siguientesPermitidos().contains(tipo)) {
            throw new TransicionInvalidaException(estado.trackingNumber(), estado.estado(), tipo,
                    estado.siguientesPermitidos());
        }

        if (estado.ultimoMovimientoEn() != null && ocurridoEn.isBefore(estado.ultimoMovimientoEn())) {
            throw new MovimientoFueraDeOrdenException(estado.trackingNumber(), ocurridoEn,
                    estado.ultimoMovimientoEn());
        }
    }

    /**
     * El primer ingreso en la ciudad de origen, el despacho desde donde está el paquete,
     * un hub en la siguiente ciudad de la ruta, y la llegada, el reparto y la entrega en
     * la ciudad de destino.
     */
    private static void validarCiudadDelCentro(EstadoDelEnvio estado, EventType tipo, Centro centro,
            Ciudad ciudadCentro) {
        CiudadEsperada esperada = FlujoLogistico.ciudadEsperada(tipo, estado.ultimoTipo());
        Ciudad debeEstarEn = estado.ciudadQueExige(esperada);
        if (!centro.getCityId().equals(debeEstarEn.id())) {
            throw CentroFueraDeCiudadException.para(tipo, esperada, centro.getName(), ciudadCentro.etiqueta(),
                    debeEstarEn.etiqueta());
        }
    }
}
