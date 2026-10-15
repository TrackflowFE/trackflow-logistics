package com.trackflow.modules.logistics.domain;

public class UnknownShipmentException extends RuntimeException {

    public UnknownShipmentException(String trackingNumber) {
        super("No existe un envío con el número de seguimiento " + trackingNumber);
    }
}
