package com.trackflow.shared.geografia;

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
class CatalogoDeCiudadesJpa implements CatalogoDeCiudades {

    private final CiudadJpaRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public Optional<Ciudad> porId(Long id) {
        return Optional.ofNullable(id).flatMap(jpa::findById).map(CiudadRegistrada::toCiudad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ciudad> buscar(String texto, int limite) {
        if (!StringUtils.hasText(texto)) {
            return List.of();
        }
        return jpa.buscar(texto.trim(), Limit.of(limite)).stream()
                .map(CiudadRegistrada::toCiudad)
                .toList();
    }
}
