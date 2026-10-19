package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.shared.events.EnvioCreadoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeguirEnvioCreado {

    private final TrackedShipmentRepository trackedShipments;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ejecutar(EnvioCreadoEvent event) {
        trackedShipments.save(new TrackedShipment(event.trackingNumber(), event.registeredAt(),
                event.originCityId(), event.destinationCityId()));
    }
}
