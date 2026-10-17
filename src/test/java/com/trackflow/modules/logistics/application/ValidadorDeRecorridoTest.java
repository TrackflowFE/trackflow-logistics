package com.trackflow.modules.logistics.application;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.CALI;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MANIZALES;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.REGISTRO;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.centro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.application.ValidadorDeRecorrido.LugarValidado;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CentroFueraDeCiudadException;
import com.trackflow.modules.logistics.domain.CentroInvalidoException;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.MovimientoAntesDelRegistroException;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.TipoCentro;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidadorDeRecorridoTest {

    private static final Instant DESPUES = REGISTRO.plusSeconds(3600);

    @Mock
    private CatalogoDeCentros centros;

    @Mock
    private CatalogoDeCiudades ciudades;

    @InjectMocks
    private ValidadorDeRecorrido validador;

    private final Centro cdMedellin = centro(10L, "CD Medellín", MEDELLIN);
    private final Centro cdBogota = centro(20L, "CD Bogotá", BOGOTA);
    private final Centro cdManizales = centro(40L, "CD Manizales", MANIZALES);

    @BeforeEach
    void catalogoDeCiudades() {
        lenient().when(ciudades.exigir(anyLong())).thenAnswer(inv -> switch (
                inv.<Long>getArgument(0).intValue()) {
            case 1 -> MEDELLIN;
            case 2 -> BOGOTA;
            case 3 -> CALI;
            case 4 -> MANIZALES;
            default -> throw new IllegalStateException();
        });
    }

    /** Estado de un envío Medellín → Bogotá por Manizales. */
    private static EstadoDelEnvio estado(EventType ultimo, EventType penultimo, Ciudad actual, Instant ultimoEn) {
        List<Ciudad> ruta = actual.equals(BOGOTA) ? List.of(BOGOTA)
                : actual.equals(MANIZALES) ? List.of(MANIZALES, BOGOTA) : List.of(MEDELLIN, MANIZALES, BOGOTA);
        Ciudad siguiente = ruta.size() == 1 ? ruta.get(0) : ruta.get(1);
        return new EstadoDelEnvio(TN, REGISTRO, ultimo, FlujoLogistico.estadoTras(ultimo, penultimo), ultimoEn,
                null, null, actual, MEDELLIN, BOGOTA, ruta, siguiente,
                FlujoLogistico.siguientesPermitidos(ultimo, siguiente.equals(BOGOTA)));
    }

    @Test
    void elPrimerIngresoEnUnCentroDelOrigenEsValido() {
        when(centros.exigir(10L)).thenReturn(cdMedellin);

        LugarValidado lugar = validador.validar(estado(null, null, MEDELLIN, null), EventType.RECEIVED_AT_CENTER,
                10L, DESPUES);

        assertThat(lugar.centro()).isSameAs(cdMedellin);
        assertThat(lugar.ciudad()).isEqualTo(MEDELLIN);
    }

    @Test
    void elIngresoAUnHubDebeSerEnLaSiguienteCiudadDeLaRuta() {
        when(centros.exigir(40L)).thenReturn(cdManizales);
        EstadoDelEnvio despachado = estado(EventType.DISPATCHED, EventType.RECEIVED_AT_CENTER, MEDELLIN, DESPUES);

        LugarValidado lugar = validador.validar(despachado, EventType.RECEIVED_AT_CENTER, 40L,
                DESPUES.plusSeconds(60));

        assertThat(lugar.ciudad()).isEqualTo(MANIZALES);
    }

    @Test
    void rechazaUnHubFueraDeLaRuta() {
        Centro cdCali = centro(30L, "CD Cali", CALI);
        when(centros.exigir(30L)).thenReturn(cdCali);
        EstadoDelEnvio despachado = estado(EventType.DISPATCHED, EventType.RECEIVED_AT_CENTER, MEDELLIN, DESPUES);

        assertThatThrownBy(() -> validador.validar(despachado, EventType.RECEIVED_AT_CENTER, 30L,
                DESPUES.plusSeconds(60)))
                .isInstanceOf(CentroFueraDeCiudadException.class)
                .hasMessageContaining("CD Cali")
                .hasMessageContaining(MANIZALES.etiqueta());
    }

    @Test
    void rechazaElPrimerIngresoFueraDelOrigen() {
        when(centros.exigir(20L)).thenReturn(cdBogota);

        assertThatThrownBy(() -> validador.validar(estado(null, null, MEDELLIN, null),
                EventType.RECEIVED_AT_CENTER, 20L, DESPUES))
                .isInstanceOf(CentroFueraDeCiudadException.class)
                .hasMessageContaining("la ciudad de origen del envío");
    }

    @Test
    void rechazaUnaTransicionFueraDelFlujoSinConsultarElCentro() {
        assertThatThrownBy(() -> validador.validar(estado(null, null, MEDELLIN, null), EventType.DELIVERED, 20L,
                DESPUES))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("REGISTERED")
                .hasMessageContaining("RECEIVED_AT_CENTER");
        verify(centros, never()).exigir(anyLong());
    }

    @Test
    void rechazaCualquierMovimientoTrasLaEntrega() {
        EstadoDelEnvio entregado = estado(EventType.DELIVERED, EventType.OUT_FOR_DELIVERY, BOGOTA, DESPUES);

        assertThatThrownBy(() -> validador.validar(entregado, EventType.RECEIVED_AT_CENTER, 20L,
                DESPUES.plusSeconds(60)))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("no admite más movimientos");
    }

    @Test
    void trasDespacharHaciaUnHubNoSePuedeLlegarAlDestino() {
        EstadoDelEnvio despachado = estado(EventType.DISPATCHED, EventType.RECEIVED_AT_CENTER, MEDELLIN, DESPUES);

        assertThatThrownBy(() -> validador.validar(despachado, EventType.ARRIVED_AT_DESTINATION_CENTER, 20L,
                DESPUES.plusSeconds(60)))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void rechazaUnMovimientoAnteriorAlRegistroDelEnvio() {
        assertThatThrownBy(() -> validador.validar(estado(null, null, MEDELLIN, null),
                EventType.RECEIVED_AT_CENTER, 10L, REGISTRO.minusSeconds(1)))
                .isInstanceOf(MovimientoAntesDelRegistroException.class)
                .hasMessageContaining(TN);
    }

    @Test
    void rechazaUnMovimientoAnteriorAlUltimoRegistrado() {
        EstadoDelEnvio recibido = estado(EventType.RECEIVED_AT_CENTER, null, MEDELLIN, DESPUES);

        assertThatThrownBy(() -> validador.validar(recibido, EventType.DISPATCHED, 10L, DESPUES.minusSeconds(1)))
                .isInstanceOf(MovimientoFueraDeOrdenException.class);
    }

    @Test
    void unMovimientoALaMismaHoraQueElUltimoEsValido() {
        when(centros.exigir(10L)).thenReturn(cdMedellin);
        EstadoDelEnvio recibido = estado(EventType.RECEIVED_AT_CENTER, null, MEDELLIN, DESPUES);

        LugarValidado lugar = validador.validar(recibido, EventType.DISPATCHED, 10L, DESPUES);

        assertThat(lugar.centro().getId()).isEqualTo(10L);
    }

    @Test
    void propagaQueElCentroNoSirve() {
        when(centros.exigir(99L)).thenThrow(CentroInvalidoException.noExiste(99L));

        assertThatThrownBy(() -> validador.validar(estado(null, null, MEDELLIN, null),
                EventType.RECEIVED_AT_CENTER, 99L, DESPUES))
                .isInstanceOf(CentroInvalidoException.class);
    }

    @Test
    void elRepartoDebeSerEnLaCiudadDeDestino() {
        Centro oficinaBogota = centro(21L, "Oficina Bogotá", TipoCentro.OFICINA, BOGOTA.id(), true);
        when(centros.exigir(21L)).thenReturn(oficinaBogota);
        EstadoDelEnvio enDestino = estado(EventType.ARRIVED_AT_DESTINATION_CENTER, EventType.DISPATCHED, BOGOTA,
                DESPUES);

        LugarValidado lugar = validador.validar(enDestino, EventType.OUT_FOR_DELIVERY, 21L,
                DESPUES.plusSeconds(1));

        assertThat(lugar.ciudad()).isEqualTo(BOGOTA);
        assertThat(lugar.centro().getType()).isEqualTo(TipoCentro.OFICINA);
    }
}
