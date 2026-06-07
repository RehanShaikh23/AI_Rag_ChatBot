package com.ragchatbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A single message within a conversation (user prompt or AI response).
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_message_conversation", columnList = "conversation_id"),
    @Index(name = "idx_message_created", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * Message role: USER, ASSISTANT, or SYSTEM.
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageRole role;

    /**
     * The text content of the message.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Optional code block (stored as JSON string).
     * Format: {"lang": "python", "content": "print('hello')"}
     */
    @Column(name = "code_block", columnDefinition = "TEXT")
    private String codeBlock;

    /**
     * Token count for monitoring usage.
     */
    @Column(name = "token_count")
    private Integer tokenCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }
}
