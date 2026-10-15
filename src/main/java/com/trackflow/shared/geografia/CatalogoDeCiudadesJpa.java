package com.trackflow.shared.geografia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class CatalogoDeCiudadesJpa implements CatalogoDeCiudades {

    private final CiudadJpaRepository jpa;

    CatalogoDeCiudadesJpa(CiudadJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ciudad> porId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpa.findById(id).map(CiudadRegistrada::toCiudad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ciudad> buscar(String texto, int limite) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return jpa.buscar(texto.trim(), Limit.of(limite)).stream()
                .map(CiudadRegistrada::toCiudad)
                .toList();
    }
}
