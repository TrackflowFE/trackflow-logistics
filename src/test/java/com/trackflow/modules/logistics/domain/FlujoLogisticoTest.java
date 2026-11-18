package com.trackflow.modules.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FlujoLogisticoTest {

    @Test
    void unEnvioRecienRegistradoSoloPuedeRecibirseEnCentro() {
        assertThat(FlujoLogistico.siguientesPermitidos(null))
                .containsExactlyInAnyOrder(EventType.RECEIVED_AT_CENTER, EventType.PICKED_UP);
    }

    @Test
    void trasUnDespachoCabenLlegadaODestinoOIngresoAHub() {
        assertThat(FlujoLogistico.siguientesPermitidos(EventType.DISPATCHED))
                .containsExactlyInAnyOrder(EventType.ARRIVED_AT_DESTINATION_CENTER, EventType.RECEIVED_AT_CENTER,
                        EventType.EXCEPTION);
    }

    @Test
    void entregadoEsTerminal() {
        assertThat(FlujoLogistico.siguientesPermitidos(EventType.DELIVERED)).isEmpty();
    }

    @Test
    void elConjuntoDevueltoNoSePuedeModificar() {
        Set<EventType> permitidos = FlujoLogistico.siguientesPermitidos(EventType.RECEIVED_AT_CENTER);

        assertThatThrownBy(() -> permitidos.add(EventType.DELIVERED))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "RECEIVED_AT_CENTER, DISPATCHED, true",
            "DISPATCHED, ARRIVED_AT_DESTINATION_CENTER, true",
            "ARRIVED_AT_DESTINATION_CENTER, OUT_FOR_DELIVERY, true",
            "OUT_FOR_DELIVERY, DELIVERED, true",
            "RECEIVED_AT_CENTER, DELIVERED, false",
            "RECEIVED_AT_CENTER, RECEIVED_AT_CENTER, false",
            "DISPATCHED, DISPATCHED, false",
            "ARRIVED_AT_DESTINATION_CENTER, DISPATCHED, false",
            "OUT_FOR_DELIVERY, RECEIVED_AT_CENTER, false",
            "DELIVERED, RECEIVED_AT_CENTER, false",
            "DELIVERED, DISPATCHED, false",
            // HU-16: detenerse cabe en cualquier punto del camino, pero no despues de entregar.
            "RECEIVED_AT_CENTER, EXCEPTION, true",
            "DISPATCHED, EXCEPTION, true",
            "OUT_FOR_DELIVERY, EXCEPTION, true",
            "DELIVERED, EXCEPTION, false"
    })
    void permiteSoloLasTransicionesDelRecorrido(EventType ultimo, EventType siguiente, boolean esperado) {
        assertThat(FlujoLogistico.permite(ultimo, siguiente)).isEqualTo(esperado);
    }

    @Test
    void sinMovimientosNoSePuedeDespacharNiEntregar() {
        assertThat(FlujoLogistico.permite(null, EventType.DISPATCHED)).isFalse();
        assertThat(FlujoLogistico.permite(null, EventType.DELIVERED)).isFalse();
        // Un envio que todavia no se ha movido no esta en manos de la operacion (HU-16).
        assertThat(FlujoLogistico.permite(null, EventType.EXCEPTION)).isFalse();
    }

    @Test
    void trasDespachoHaciaElDestinoSoloCabeLaLlegada() {
        assertThat(FlujoLogistico.siguientesPermitidos(EventType.DISPATCHED, true))
                .containsExactlyInAnyOrder(EventType.ARRIVED_AT_DESTINATION_CENTER, EventType.EXCEPTION);
    }

    @Test
    void trasDespachoHaciaUnHubSoloCabeElIngresoAlHub() {
        assertThat(FlujoLogistico.siguientesPermitidos(EventType.DISPATCHED, false))
                .containsExactlyInAnyOrder(EventType.RECEIVED_AT_CENTER, EventType.EXCEPTION);
    }

    @Test
    void laRutaNoCambiaLasTransicionesQueNoSonDespacho() {
        assertThat(FlujoLogistico.siguientesPermitidos(EventType.RECEIVED_AT_CENTER, true))
                .containsExactlyInAnyOrder(EventType.DISPATCHED, EventType.EXCEPTION);
        assertThat(FlujoLogistico.siguientesPermitidos(null, false))
                .containsExactlyInAnyOrder(EventType.RECEIVED_AT_CENTER, EventType.PICKED_UP);
    }

    @Test
    void sinMovimientosElEstadoEsRegistrado() {
        assertThat(FlujoLogistico.estadoTras(null, null)).isEqualTo(FlujoLogistico.ESTADO_REGISTRADO);
    }

    @Test
    void elPrimerIngresoDejaElEnvioEnCentroDeDistribucion() {
        assertThat(FlujoLogistico.estadoTras(EventType.RECEIVED_AT_CENTER, null))
                .isEqualTo("AT_DISTRIBUTION_CENTER");
    }

    @Test
    void unIngresoAHubDejaElEnvioEnTransito() {
        assertThat(FlujoLogistico.estadoResultante(EventType.RECEIVED_AT_CENTER, EventType.DISPATCHED))
                .isEqualTo("IN_TRANSIT");
    }

    @Test
    void recogidaYRecepcionEnOrigenNoSonTransito() {
        assertThat(FlujoLogistico.permite(null, EventType.PICKED_UP)).isTrue();
        assertThat(FlujoLogistico.siguientesPermitidos(EventType.PICKED_UP))
                .containsExactlyInAnyOrder(EventType.RECEIVED_AT_CENTER, EventType.EXCEPTION);
        assertThat(FlujoLogistico.estadoResultante(EventType.PICKED_UP, null)).isEqualTo("IN_CUSTODY");
        assertThat(FlujoLogistico.estadoResultante(EventType.RECEIVED_AT_CENTER, EventType.PICKED_UP))
                .isEqualTo("AT_DISTRIBUTION_CENTER");
        assertThat(FlujoLogistico.ciudadEsperada(EventType.RECEIVED_AT_CENTER, EventType.PICKED_UP))
                .isEqualTo(CiudadEsperada.ORIGEN);
    }

    @Test
    void losDemasMovimientosDejanSuPropioEstado() {
        assertThat(FlujoLogistico.estadoResultante(EventType.DISPATCHED, EventType.RECEIVED_AT_CENTER))
                .isEqualTo("IN_TRANSIT");
        assertThat(FlujoLogistico.estadoResultante(EventType.ARRIVED_AT_DESTINATION_CENTER, EventType.DISPATCHED))
                .isEqualTo("AT_DESTINATION_CENTER");
        assertThat(FlujoLogistico.estadoTras(EventType.DELIVERED, EventType.OUT_FOR_DELIVERY))
                .isEqualTo("DELIVERED");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "RECEIVED_AT_CENTER, NULL, ORIGEN",
            "RECEIVED_AT_CENTER, DISPATCHED, SIGUIENTE_PARADA",
            "DISPATCHED, RECEIVED_AT_CENTER, ACTUAL",
            "ARRIVED_AT_DESTINATION_CENTER, DISPATCHED, DESTINO",
            "OUT_FOR_DELIVERY, ARRIVED_AT_DESTINATION_CENTER, DESTINO",
            "DELIVERED, OUT_FOR_DELIVERY, DESTINO"
    })
    void cadaMovimientoExigeSuCiudad(EventType tipo, EventType ultimo, CiudadEsperada esperada) {
        assertThat(FlujoLogistico.ciudadEsperada(tipo, ultimo)).isEqualTo(esperada);
    }

    @Test
    void cadaTipoDeEventoTieneEtiquetaYEstadoResultante() {
        assertThat(EventType.OUT_FOR_DELIVERY.etiqueta()).isEqualTo("Salida a reparto");
        assertThat(EventType.OUT_FOR_DELIVERY.resultingStatus()).isEqualTo("OUT_FOR_DELIVERY");
        assertThat(CiudadEsperada.SIGUIENTE_PARADA.descripcion()).contains("siguiente ciudad");
    }
}
