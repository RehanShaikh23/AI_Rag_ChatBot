package com.ragchatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI RAG ChatBot — Main Application.
 * <p>
 * Production-ready Spring Boot backend powered by Spring AI
 * with NVIDIA NIM integration, RAG (PGVector), JWT auth,
 * SSE streaming, and full REST API.
 */
@SpringBootApplication
@EnableAsync
public class RagChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagChatbotApplication.class, args);
    }
}
