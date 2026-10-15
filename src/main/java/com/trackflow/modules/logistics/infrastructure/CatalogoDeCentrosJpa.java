package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.application.CatalogoDeCentros;
import com.trackflow.modules.logistics.domain.Centro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class CatalogoDeCentrosJpa implements CatalogoDeCentros {

    private final CentroJpaRepository jpa;

    CatalogoDeCentrosJpa(CentroJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Centro> porId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpa.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Centro> buscar(String texto, Long ciudadId, int limite) {
        String textoNormalizado = (texto == null || texto.isBlank()) ? null : texto.trim();
        return jpa.buscar(textoNormalizado, ciudadId, Limit.of(limite));
    }
}
