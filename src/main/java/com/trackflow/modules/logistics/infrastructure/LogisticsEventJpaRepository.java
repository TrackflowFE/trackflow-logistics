package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.domain.LogisticsEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticsEventJpaRepository extends JpaRepository<LogisticsEvent, Long> {

    /**
     * Ordenado por cuándo ocurrió y, a igual hora, por id, que es el orden de registro.
     * Sin el segundo criterio, dos movimientos reportados con la misma hora (lo normal si
     * se indica la fecha con precisión de minutos) salían en cualquier orden, y quien toma
     * el último de la lista como "último movimiento" podía tomar el que no era.
     */
    List<LogisticsEvent> findByTrackingNumberOrderByOccurredAtAscIdAsc(String trackingNumber);

    boolean existsByEventId(String eventId);

    Optional<LogisticsEvent> findByEventId(String eventId);

    List<LogisticsEvent> findAllByOrderByOccurredAtAscIdAsc();
}
