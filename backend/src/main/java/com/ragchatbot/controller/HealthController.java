package com.ragchatbot.controller;

import com.ragchatbot.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "Application health check")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Check if the API is running")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("status", "UP", "service", "AI RAG ChatBot Backend"),
                "Service is healthy"));
    }
}
