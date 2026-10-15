package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.application.RedDeRutas;
import com.trackflow.modules.logistics.domain.Tramo;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class RedDeRutasJpa implements RedDeRutas {

    private final TramoDeRutaJpaRepository jpa;

    RedDeRutasJpa(TramoDeRutaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tramo> tramosActivos() {
        return jpa.activosEntreCiudadesConCobertura().stream()
                .map(TramoDeRuta::toTramo)
                .toList();
    }
}
