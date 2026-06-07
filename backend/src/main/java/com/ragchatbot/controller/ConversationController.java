package com.ragchatbot.controller;

import com.ragchatbot.dto.ApiResponse;
import com.ragchatbot.dto.conversation.ConversationDto;
import com.ragchatbot.entity.User;
import com.ragchatbot.service.AuthService;
import com.ragchatbot.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Manage conversation threads")
public class ConversationController {

    private final ConversationService conversationService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List user's conversations (paginated)")
    public ResponseEntity<ApiResponse<Page<ConversationDto>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        Page<ConversationDto> conversations = conversationService.getConversations(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(conversations));
    }

    @PostMapping
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        String title = body != null ? body.get("title") : null;
        var conversation = conversationService.createConversation(user, title);
        ConversationDto dto = ConversationDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .messageCount(0)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(dto, "Conversation created"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update conversation title")
    public ResponseEntity<ApiResponse<ConversationDto>> updateTitle(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        String title = body.get("title");
        ConversationDto updated = conversationService.updateTitle(id, user.getId(), title);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Title updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a conversation and all messages")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long id,
            Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        conversationService.deleteConversation(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Conversation deleted"));
    }
}
