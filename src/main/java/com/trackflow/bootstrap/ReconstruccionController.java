package com.trackflow.bootstrap;

import com.trackflow.modules.logistics.application.RepublicarEventosLogisticos;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vuelve a publicar en RabbitMQ todos los eventos logísticos registrados, en el orden en
 * que ocurrieron, para que las APIs que los consumen (Envíos y Reportes) reconstruyan su
 * estado si quedó desincronizado.
 *
 * Los envíos los republica la API de Envíos con su propio endpoint, y conviene hacerlo
 * antes que esto: un movimiento de un envío que la otra API todavía no conoce se reintenta.
 */
@RestController
@RequestMapping("/api/admin")
public class ReconstruccionController {

    private final RepublicarEventosLogisticos eventos;

    public ReconstruccionController(RepublicarEventosLogisticos eventos) {
        this.eventos = eventos;
    }

    @PostMapping("/rebuild-projections")
    public Map<String, Integer> reconstruir() {
        return Map.of("eventsRepublished", eventos.ejecutar());
    }
}
