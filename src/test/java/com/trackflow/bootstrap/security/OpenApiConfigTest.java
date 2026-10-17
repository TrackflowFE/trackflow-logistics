package com.trackflow.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void declaraElEsquemaBearerYLoExigeEnLaDocumentacion() {
        OpenAPI api = new OpenApiConfig().trackflowOpenApi();

        assertThat(api.getInfo().getTitle()).isEqualTo("TrackFlow Logistics API");
        SecurityScheme esquema = api.getComponents().getSecuritySchemes().get("TokenDeOperador");
        assertThat(esquema.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(esquema.getScheme()).isEqualTo("bearer");
        assertThat(esquema.getBearerFormat()).isEqualTo("JWT");
        assertThat(api.getSecurity()).singleElement()
                .satisfies(requisito -> assertThat(requisito).containsKey("TokenDeOperador"));
    }
}
