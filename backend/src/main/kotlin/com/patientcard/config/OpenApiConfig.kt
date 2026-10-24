package com.patientcard.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Private AI Patient Card API")
                    .version("0.1.0")
                    .description(
                        "Backend prywatnej karty pacjenta. API aplikacji: /api/v1/app, API AI: /api/v1/ai. " +
                            "W ETAPIE 1 AI korzysta wyłącznie z MockAiProvider.",
                    ),
            )
            .components(
                Components().addSecuritySchemes(
                    schemeName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
            .addSecurityItem(SecurityRequirement().addList(schemeName))
    }
}
