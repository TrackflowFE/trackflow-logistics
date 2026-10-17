package com.trackflow.modules.logistics.infrastructure;

import static com.trackflow.modules.logistics.domain.DatosDePrueba.BOGOTA;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.MEDELLIN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.TN;
import static com.trackflow.modules.logistics.domain.DatosDePrueba.envio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.logistics.domain.TrackedShipment;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaTrackedShipmentRepositoryTest {

    @Mock
    private TrackedShipmentJpaRepository jpa;

    @InjectMocks
    private JpaTrackedShipmentRepository repositorio;

    private final TrackedShipment envio = envio(MEDELLIN, BOGOTA);

    @Test
    void saveDelegaEnLaBase() {
        when(jpa.save(envio)).thenReturn(envio);

        assertThat(repositorio.save(envio)).isSameAs(envio);
    }

    @Test
    void existsConsultaPorNumeroDeGuia() {
        when(jpa.existsById(TN)).thenReturn(true);

        assertThat(repositorio.exists(TN)).isTrue();
        assertThat(repositorio.exists("OTRO")).isFalse();
    }

    @Test
    void porTrackingNumberNoBloquea() {
        when(jpa.findById(TN)).thenReturn(Optional.of(envio));

        assertThat(repositorio.porTrackingNumber(TN)).containsSame(envio);
    }

    @Test
    void paraActualizarUsaLaConsultaConBloqueo() {
        when(jpa.bloquearPorTrackingNumber(TN)).thenReturn(Optional.of(envio));

        assertThat(repositorio.porTrackingNumberParaActualizar(TN)).containsSame(envio);
        verify(jpa).bloquearPorTrackingNumber(TN);
    }
}
