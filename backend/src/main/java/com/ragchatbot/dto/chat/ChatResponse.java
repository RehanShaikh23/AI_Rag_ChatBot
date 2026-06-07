package com.ragchatbot.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private Long messageId;
    private Long conversationId;
    private String conversationTitle;
    private String role;
    private String content;
    private CodeBlock codeBlock;
    private Integer tokenCount;
    private boolean ragUsed;
    private LocalDateTime createdAt;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class CodeBlock {
        private String lang;
        private String content;
    }
}
