package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.application.CatalogoDeCentros;
import com.trackflow.modules.logistics.domain.Centro;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class CatalogoDeCentrosJpa implements CatalogoDeCentros {

    private final CentroJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<Centro> porId(Long id) {
        return Optional.ofNullable(id).flatMap(jpa::findById);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Centro> buscar(String texto, Long ciudadId, int limite) {
        String textoNormalizado = Optional.ofNullable(texto).filter(StringUtils::hasText).map(String::trim).orElse(null);
        return jpa.buscar(textoNormalizado, ciudadId, Limit.of(limite));
    }
}
