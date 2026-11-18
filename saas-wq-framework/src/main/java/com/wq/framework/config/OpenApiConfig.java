package com.wq.framework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 metadata for the public API documentation.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    /**
     * Declares the API document including the bearer authentication scheme so that
     * Swagger UI can call protected endpoints directly.
     *
     * @return OpenAPI definition
     */
    @Bean
    public OpenAPI saasWqOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("saas-wq API")
                        .description("Multi-tenant SaaS backend: tenant registry, users and customers")
                        .version("1.0.0")
                        .contact(new Contact().name("wangqi").url("https://github.com/wangqi"))
                        .license(new License().name("MIT License")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
