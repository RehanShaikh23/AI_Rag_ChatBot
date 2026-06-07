package com.ragchatbot.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ChatRequest {

    /**
     * If null, a new conversation is auto-created.
     */
    private Long conversationId;

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 10000, message = "Message must be under 10,000 characters")
    private String message;

    /**
     * Whether to include RAG context from uploaded documents.
     */
    private boolean useRag = true;
}
