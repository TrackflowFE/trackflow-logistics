package com.trackflow.modules.logistics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TramoDeRutaJpaRepository extends JpaRepository<TramoDeRuta, Long> {

    /** Solo tramos activos cuyas dos puntas tienen al menos un centro activo. */
    @Query("""
            select t from TramoDeRuta t
            where t.active = true
              and exists (select 1 from Centro c where c.cityId = t.fromCityId and c.active = true)
              and exists (select 1 from Centro c where c.cityId = t.toCityId and c.active = true)
            """)
    List<TramoDeRuta> activosEntreCiudadesConCobertura();
}
