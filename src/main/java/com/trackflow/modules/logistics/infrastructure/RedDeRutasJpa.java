package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.application.RedDeRutas;
import com.trackflow.modules.logistics.domain.Tramo;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class RedDeRutasJpa implements RedDeRutas {

    private final TramoDeRutaJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public List<Tramo> tramosActivos() {
        return jpa.activosEntreCiudadesConCobertura().stream()
                .map(TramoDeRuta::toTramo)
                .toList();
    }
}
