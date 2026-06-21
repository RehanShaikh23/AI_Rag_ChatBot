package com.ragchatbot.controller;

import com.ragchatbot.ai.RagService;
import com.ragchatbot.dto.ApiResponse;
import com.ragchatbot.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debug endpoint to verify that the RAG pipeline is working end-to-end.
 * <p>
 * Use this to test similarity search independently from the chat flow.
 * Requires authentication (same JWT as chat endpoints).
 */
@Profile("dev")
@RestController
@RequestMapping("/api/debug/rag")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RAG Debug", description = "Diagnose the RAG pipeline — search, inspect context")
public class RagDebugController {

    private final RagService ragService;
    private final AuthService authService;

    /**
     * Test a RAG similarity search without triggering the full chat flow.
     * Returns the retrieved context, its length, and a preview.
     */
    @PostMapping("/search")
    @Operation(summary = "Test RAG similarity search for a given query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testSearch(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String query = body.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.ok(Map.of("error", "Missing 'query' field in request body")));
        }

        var user = authService.getUserByEmail(auth.getName());

        log.info("🧪 RAG debug search — query: '{}', user: {}", query, user.getId());

        String context = ragService.searchRelevantContext(query, user.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("userId", user.getId());
        result.put("contextFound", context != null);
        result.put("contextLength", context != null ? context.length() : 0);
        result.put("contextPreview", context != null
                ? context.substring(0, Math.min(context.length(), 500))
                : "NO RESULTS — check logs for details");

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
