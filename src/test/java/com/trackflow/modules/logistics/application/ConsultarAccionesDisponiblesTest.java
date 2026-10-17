package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MANIZALES;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.application.ConsultarAccionesDisponibles.Accion;
import com.trackflow.modules.logistics.application.ConsultarAccionesDisponibles.Acciones;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarAccionesDisponiblesTest {

    @Mock
    private ConsultarEstadoDelEnvio estadoDelEnvio;

    @Mock
    private CatalogoDeCentros centros;

    @InjectMocks
    private ConsultarAccionesDisponibles consulta;

    private final Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);
    private final Centro cdManizales = centro(40L, "CD Manizales", MANIZALES);

    private static EstadoDelEnvio estado(EventType ultimo, EventType penultimo, Ciudad actual, List<Ciudad> ruta) {
        Ciudad siguiente = ruta.size() == 1 ? ruta.get(0) : ruta.get(1);
        return new EstadoDelEnvio(TN, REGISTRO, ultimo, FlujoLogistico.estadoTras(ultimo, penultimo), null, null,
                null, actual, MEDELLIN, BOGOTA, ruta, siguiente,
                FlujoLogistico.siguientesPermitidos(ultimo, siguiente.equals(BOGOTA)));
    }

    @Test
    void unEnvioRegistradoOfreceRecibirEnLosCentrosDelOrigen() {
        EstadoDelEnvio estado = estado(null, null, MEDELLIN, List.of(MEDELLIN, MANIZALES, BOGOTA));
        when(estadoDelEnvio.ejecutar(TN)).thenReturn(estado);
        when(centros.buscar(isNull(), anyLong(), anyInt())).thenReturn(List.of(cdMedellin));

        Acciones acciones = consulta.ejecutar(TN);

        assertThat(acciones.estado()).isSameAs(estado);
        assertThat(acciones.acciones()).hasSize(1);
        Accion accion = acciones.acciones().get(0);
        assertThat(accion.tipo()).isEqualTo(EventType.RECEIVED_AT_CENTER);
        assertThat(accion.ciudadEsperada()).isEqualTo(CiudadEsperada.ORIGEN);
        assertThat(accion.ciudadDeLosCentros()).isEqualTo(MEDELLIN);
        assertThat(accion.centros()).singleElement().satisfies(disponible -> {
            assertThat(disponible.centro()).isSameAs(cdMedellin);
            assertThat(disponible.ciudad()).isEqualTo(MEDELLIN);
        });
        assertThat(accion.ruta()).isNull();
    }

    @Test
    void elDespachoMuestraLaRutaPlaneadaCompleta() {
        when(estadoDelEnvio.ejecutar(TN)).thenReturn(estado(EventType.RECEIVED_AT_CENTER, null, MEDELLIN,
                List.of(MEDELLIN, MANIZALES, BOGOTA)));
        when(centros.buscar(null, MEDELLIN.id(), 20)).thenReturn(List.of(cdMedellin));

        Accion accion = consulta.ejecutar(TN).acciones().get(0);

        assertThat(accion.tipo()).isEqualTo(EventType.DISPATCHED);
        assertThat(accion.ciudadEsperada()).isEqualTo(CiudadEsperada.ACTUAL);
        assertThat(accion.ruta()).isEqualTo(
                "MEDELLÍN - ANTIOQUIA → MANIZALES - CALDAS → BOGOTÁ D.C. - BOGOTÁ");
    }

    @Test
    void trasDespacharHaciaUnHubSoloOfreceLosCentrosDelHub() {
        when(estadoDelEnvio.ejecutar(TN)).thenReturn(estado(EventType.DISPATCHED, EventType.RECEIVED_AT_CENTER,
                MEDELLIN, List.of(MEDELLIN, MANIZALES, BOGOTA)));
        when(centros.buscar(null, MANIZALES.id(), 20)).thenReturn(List.of(cdManizales));

        Acciones acciones = consulta.ejecutar(TN);

        assertThat(acciones.acciones()).singleElement().satisfies(accion -> {
            assertThat(accion.tipo()).isEqualTo(EventType.RECEIVED_AT_CENTER);
            assertThat(accion.ciudadEsperada()).isEqualTo(CiudadEsperada.SIGUIENTE_PARADA);
            assertThat(accion.ciudadDeLosCentros()).isEqualTo(MANIZALES);
        });
    }

    @Test
    void unEnvioEntregadoNoTieneAcciones() {
        when(estadoDelEnvio.ejecutar(TN)).thenReturn(estado(EventType.DELIVERED, EventType.OUT_FOR_DELIVERY,
                BOGOTA, List.of(BOGOTA)));

        Acciones acciones = consulta.ejecutar(TN);

        assertThat(acciones.acciones()).isEmpty();
        verifyNoInteractions(centros);
    }

    @Test
    void unEnvioDesconocidoFalla() {
        when(estadoDelEnvio.ejecutar("X")).thenThrow(new UnknownShipmentException("X"));

        assertThatThrownBy(() -> consulta.ejecutar("X")).isInstanceOf(UnknownShipmentException.class);
    }

    @Test
    void laEtiquetaDeUnaRutaDeUnaCiudadEsEsaCiudad() {
        assertThat(ConsultarAccionesDisponibles.etiquetaDeRuta(List.of(BOGOTA))).isEqualTo(BOGOTA.etiqueta());
        assertThat(ConsultarAccionesDisponibles.etiquetaDeRuta(List.of())).isEmpty();
    }
}
