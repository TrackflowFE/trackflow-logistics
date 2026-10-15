package com.trackflow.modules.logistics.application;

/**
 * Puerto de salida hacia el broker. Aísla a la capa de aplicación de RabbitMQ:
 * cambiar de broker es escribir otro adaptador en infrastructure.
 */
public interface EventoLogisticoPublisher {

    void publicar(EventoLogisticoEntrante evento);
}
