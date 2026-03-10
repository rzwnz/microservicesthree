package com.sthree.file.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.1 configuration exposed via Scalar UI.
 *
 * Scalar replaces the legacy Swagger UI with a modern, fast,
 * and beautiful API reference page.
 *
 * <ul>
 *   <li>OpenAPI JSON: <code>/v3/api-docs</code></li>
 *   <li>Scalar UI:    <code>/scalar.html</code></li>
 * </ul>
 *
 * @author rzwnz
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8087}")
    private int serverPort;

    @Bean
    public OpenAPI fileServiceOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("File Service API")
                        .description("""
                                RESTful API for the **File Service** — a domain-agnostic object storage \
                                gateway backed by Garage (S3-compatible).
                                
                                ### Features
                                - Direct multipart upload and presigned-URL upload flows
                                - File metadata, thumbnails, and streaming downloads
                                - Shareable links with optional password protection
                                - Per-user storage quotas
                                - Soft-delete with configurable retention
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("rzwnz")
                                .url("https://github.com/rzwnz"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("UUID / JWT")
                                        .description("Pass a valid Bearer token (UUID in dev mode).")));
    }
}
