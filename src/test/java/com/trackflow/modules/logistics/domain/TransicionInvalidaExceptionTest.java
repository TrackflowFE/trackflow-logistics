package com.trackflow.modules.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransicionInvalidaExceptionTest {

    @Test
    void enEstadoTerminalDiceQueNoAdmiteMasMovimientos() {
        TransicionInvalidaException e = new TransicionInvalidaException("TF-1", "DELIVERED",
                EventType.DISPATCHED, Set.of());

        assertThat(e.getMessage())
                .isEqualTo("El envío TF-1 ya está en estado DELIVERED y no admite más movimientos; "
                        + "se intentó registrar DISPATCHED");
    }

    @Test
    void conAlternativasLasLista() {
        TransicionInvalidaException e = new TransicionInvalidaException("TF-1", "IN_TRANSIT",
                EventType.DELIVERED,
                EnumSet.of(EventType.RECEIVED_AT_CENTER, EventType.ARRIVED_AT_DESTINATION_CENTER));

        assertThat(e.getMessage())
                .contains("no admite un movimiento DELIVERED")
                .endsWith("Ahora solo es válido: RECEIVED_AT_CENTER, ARRIVED_AT_DESTINATION_CENTER");
    }

    @Test
    void elCentroFueraDeCiudadExplicaQueCiudadSeEsperaba() {
        CentroFueraDeCiudadException e = CentroFueraDeCiudadException.para(EventType.DELIVERED,
                CiudadEsperada.DESTINO, "CD Cali", "CALI - VALLE", "BOGOTÁ D.C. - BOGOTÁ");

        assertThat(e.getMessage()).isEqualTo("El centro 'CD Cali' está en CALI - VALLE, pero un movimiento "
                + "DELIVERED debe registrarse en un centro de BOGOTÁ D.C. - BOGOTÁ (la ciudad de destino del envío)");
    }

    @Test
    void elCentroInvalidoDistingueInexistenteDeInactivo() {
        assertThat(CentroInvalidoException.noExiste(7L).getMessage()).startsWith("El centro 7 no existe");
        assertThat(CentroInvalidoException.inactivo(7L, "CD Viejo").getMessage())
                .contains("'CD Viejo' (id 7) está inactivo");
    }
}
