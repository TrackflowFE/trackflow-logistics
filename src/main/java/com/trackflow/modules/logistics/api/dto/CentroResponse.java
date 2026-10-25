package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.shared.geografia.Ciudad;

public record CentroResponse(Long id, String name, String type, Long cityId, String cityName,
        String label) {

    public static CentroResponse from(Centro centro, Ciudad ciudad) {
        return new CentroResponse(
                centro.getId(),
                centro.getName(),
                centro.getType().enIngles(),
                ciudad.id(),
                ciudad.etiqueta(),
                centro.getName() + " · " + ciudad.etiqueta());
    }
}
