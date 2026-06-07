package com.ragchatbot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration with JWT bearer auth.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI RAG ChatBot API")
                        .version("1.0.0")
                        .description("""
                                Production-ready REST API for AI RAG ChatBot.
                                Powered by Spring AI with NVIDIA NIM, PostgreSQL + PGVector,
                                JWT authentication, and SSE streaming.
                                """)
                        .contact(new Contact()
                                .name("AI RAG ChatBot")
                                .url("https://github.com/RehanShaikh23/AI_Rag_ChatBot")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .bearerFormat("JWT")
                                        .scheme("bearer")
                                        .description("Enter your JWT token")));
    }
}
