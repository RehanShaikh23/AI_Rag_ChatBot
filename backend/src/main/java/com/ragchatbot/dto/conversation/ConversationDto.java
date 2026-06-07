package com.ragchatbot.dto.conversation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationDto {

    private Long id;
    private String title;
    private String lastMessage;
    private long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
