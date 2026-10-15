package com.trackflow.shared.geografia;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CiudadJpaRepository extends JpaRepository<CiudadRegistrada, Long> {

    /**
     * Las coincidencias que empiezan por el texto van primero: quien escribe "bogo"
     * espera ver "BOGOTÁ D.C." antes que "PUERTO BOGOTÁ".
     *
     * unaccent(...) quita tildes de los dos lados de la comparación: sin él, escribir
     * "bogota" sin tilde no encontraba "BOGOTÁ", porque upper() no normaliza acentos y
     * casi nadie los escribe en un buscador.
     */
    @Query("""
            select c from CiudadRegistrada c
            where cast(function('unaccent', upper(c.name)) as String)
                  like cast(function('unaccent', upper(concat('%', :texto, '%'))) as String)
               or cast(function('unaccent', upper(c.department)) as String)
                  like cast(function('unaccent', upper(concat('%', :texto, '%'))) as String)
            order by case when cast(function('unaccent', upper(c.name)) as String)
                                like cast(function('unaccent', upper(concat(:texto, '%'))) as String)
                          then 0 else 1 end,
                     c.name
            """)
    List<CiudadRegistrada> buscar(String texto, Limit limite);
}
