package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.domain.TrackedShipment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackedShipmentJpaRepository extends JpaRepository<TrackedShipment, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TrackedShipment t where t.trackingNumber = :trackingNumber")
    Optional<TrackedShipment> bloquearPorTrackingNumber(@Param("trackingNumber") String trackingNumber);
}
