package com.ragchatbot.controller;

import com.ragchatbot.dto.ApiResponse;
import com.ragchatbot.dto.chat.ChatRequest;
import com.ragchatbot.dto.chat.ChatResponse;
import com.ragchatbot.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "AI chat — send messages and receive responses")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Send a message and receive a complete AI response")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {
        ChatResponse response = chatService.sendMessage(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a message and receive a streamed AI response via SSE")
    public ResponseBodyEmitter streamMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {

        // 5-minute timeout — long enough for large responses
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(300_000L);

        ChatService.StreamContext ctx = chatService.prepareStream(request, authentication.getName());
        StringBuilder fullResponse = new StringBuilder();

        // ResponseBodyEmitter writes raw bytes and flushes after each send().
        // We manually format SSE events ("data: ...\n\n") to preserve spaces
        // in tokens — SseEmitter's event().data() strips the first space per SSE spec.
        ctx.stream().subscribe(
                // onNext — send each token as an SSE event immediately
                token -> {
                    try {
                        fullResponse.append(token);
                        String escapedToken = token.replace("\n", "\\n");
                        emitter.send("data: " + escapedToken + "\n\n", MediaType.TEXT_PLAIN);
                    } catch (IOException e) {
                        log.warn("SSE send failed (client likely disconnected): {}", e.getMessage());
                        emitter.completeWithError(e);
                    }
                },
                // onError — signal the error and close the connection
                error -> {
                    log.error("Stream error: {}", error.getMessage());
                    try {
                        emitter.send("data: [ERROR]\n\n", MediaType.TEXT_PLAIN);
                    } catch (IOException ignored) {}
                    emitter.completeWithError(error);
                },
                // onComplete — save response, send metadata, close cleanly
                () -> {
                    try {
                        chatService.saveStreamedResponse(ctx.conversationId(), fullResponse.toString());
                        emitter.send("data: [DONE]\n\n", MediaType.TEXT_PLAIN);
                        emitter.send("data: {\"conversationId\":" + ctx.conversationId() + "}\n\n",
                                     MediaType.TEXT_PLAIN);
                        emitter.complete();
                    } catch (IOException e) {
                        log.warn("SSE completion failed: {}", e.getMessage());
                        emitter.completeWithError(e);
                    }
                }
        );

        // Clean up if the client disconnects early
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out for conversation {}", ctx.conversationId());
            emitter.complete();
        });

        return emitter;
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get all messages for a conversation")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication) {
        List<ChatResponse> messages = chatService.getMessages(conversationId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }
}

