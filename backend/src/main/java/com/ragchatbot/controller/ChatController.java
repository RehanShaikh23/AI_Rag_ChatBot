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
import reactor.core.publisher.Flux;

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
    public Flux<String> streamMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {

        ChatService.StreamContext ctx = chatService.prepareStream(request, authentication.getName());
        StringBuilder fullResponse = new StringBuilder();

        return ctx.stream()
                .doOnNext(token -> fullResponse.append(token))
                .map(token -> "data: " + token.replace("\n", "\\n") + "\n\n")
                .concatWith(Flux.defer(() -> {
                    // Save the completed response after stream ends
                    chatService.saveStreamedResponse(ctx.conversationId(), fullResponse.toString());
                    return Flux.just("data: [DONE]\n\ndata: {\"conversationId\":" + ctx.conversationId() + "}\n\n");
                }))
                .doOnError(e -> log.error("Stream error: {}", e.getMessage()));
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
