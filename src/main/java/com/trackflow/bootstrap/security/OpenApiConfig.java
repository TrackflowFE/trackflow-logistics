package com.trackflow.bootstrap.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el esquema de token en la documentación, para que Swagger muestre el botón
 * "Authorize" y se puedan probar los endpoints protegidos desde el navegador.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "TokenDeOperador";

    @Bean
    OpenAPI trackflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrackFlow Logistics API")
                        .version("v1")
                        .description("""
                                API 3 de TrackFlow: registro de eventos logísticos (HU-02), historial (HU-04), \
                                itinerario del envío y red de centros.

                                El registro de un evento se admite de forma asíncrona: responde 202 y el \
                                movimiento se guarda al consumir el mensaje de la cola.

                                El historial y el itinerario son públicos. Para registrar eventos hay que \
                                obtener un token en POST /api/auth/login de la API de Envíos y acceso y usarlo \
                                con el botón Authorize."""))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Sin este requisito, el botón Authorize no adjunta el token a las peticiones.
                // Los endpoints públicos siguen funcionando sin él: quien decide es Spring Security.
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
