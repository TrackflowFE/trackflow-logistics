package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.application.TrackedShipmentRepository;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaTrackedShipmentRepository implements TrackedShipmentRepository {

    private final TrackedShipmentJpaRepository jpa;

    @Override
    public TrackedShipment save(TrackedShipment trackedShipment) {
        return jpa.save(trackedShipment);
    }

    @Override
    public boolean exists(String trackingNumber) {
        return jpa.existsById(trackingNumber);
    }

    @Override
    public Optional<TrackedShipment> porTrackingNumber(String trackingNumber) {
        return jpa.findById(trackingNumber);
    }

    @Override
    public Optional<TrackedShipment> porTrackingNumberParaActualizar(String trackingNumber) {
        return jpa.bloquearPorTrackingNumber(trackingNumber);
    }
}
