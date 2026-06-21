package com.ragchatbot.controller;

import com.ragchatbot.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for public endpoints (health, auth) that
 * do NOT require authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")  // Uses H2 — no PostgreSQL needed
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/health — returns 200 with status UP")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("POST /api/auth/login — returns 401 for bad credentials")
    void loginBadCredentials() throws Exception {
        String loginJson = """
                {
                  "email": "nonexistent@test.com",
                  "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/register — returns 400 for invalid request")
    void registerInvalidRequest() throws Exception {
        String invalidJson = """
                {
                  "email": "",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/chat — returns 401 without JWT")
    void chatEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().is(403)); // Spring Security returns 403 for missing auth
    }
}
