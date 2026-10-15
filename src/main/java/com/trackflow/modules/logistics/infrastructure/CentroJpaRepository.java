package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.domain.Centro;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CentroJpaRepository extends JpaRepository<Centro, Long> {

    /**
     * texto y ciudadId son independientes: se puede buscar solo por texto, solo por
     * ciudad, o combinando los dos. Solo devuelve centros activos — uno inactivo no
     * sirve para registrar un evento nuevo, así que tampoco tiene sentido ofrecerlo
     * en el autocompletado.
     *
     * El cast de :texto no es adorno. Cuando llega null, Postgres no tiene de dónde
     * deducir el tipo del parámetro dentro de upper(...) y lo asume bytea, con lo que
     * la consulta ni siquiera se planifica ("function upper(bytea) does not exist").
     * Decirle que es texto basta para que la rama del null funcione.
     *
     * unaccent(...) quita tildes de los dos lados de la comparación, mismo motivo que
     * en CiudadJpaRepository: sin él, "bogota" sin tilde no encontraba un centro de
     * "BOGOTÁ".
     */
    @Query("""
            select c from Centro c
            where (cast(:texto as String) is null
                   or cast(function('unaccent', upper(c.name)) as String)
                      like cast(function('unaccent', upper(concat('%', cast(:texto as String), '%'))) as String))
              and (:ciudadId is null or c.cityId = :ciudadId)
              and c.active = true
            order by case when cast(:texto as String) is not null
                           and cast(function('unaccent', upper(c.name)) as String)
                               like cast(function('unaccent', upper(concat(cast(:texto as String), '%'))) as String)
                          then 0 else 1 end,
                     c.name
            """)
    List<Centro> buscar(@Param("texto") String texto, @Param("ciudadId") Long ciudadId, Limit limite);

    boolean existsByCityIdAndActiveTrue(Long cityId);

    @Query("select distinct c.cityId from Centro c where c.active = true")
    Set<Long> ciudadesConCentroActivo();
}
