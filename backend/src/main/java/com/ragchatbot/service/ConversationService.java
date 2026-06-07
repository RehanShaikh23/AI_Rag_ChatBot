package com.ragchatbot.service;

import com.ragchatbot.dto.conversation.ConversationDto;
import com.ragchatbot.entity.ChatMessage;
import com.ragchatbot.entity.Conversation;
import com.ragchatbot.entity.User;
import com.ragchatbot.repository.ChatMessageRepository;
import com.ragchatbot.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages conversation lifecycle — create, list, update title, delete.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Create a new conversation for a user.
     */
    @Transactional
    public Conversation createConversation(User user, String title) {
        Conversation conversation = Conversation.builder()
                .user(user)
                .title(title != null ? title : "New Chat")
                .build();
        conversation = conversationRepository.save(conversation);
        log.info("Created conversation {} for user {}", conversation.getId(), user.getId());
        return conversation;
    }

    /**
     * List conversations for a user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ConversationDto> getConversations(Long userId, int page, int size) {
        Page<Conversation> conversations = conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(page, size));

        return conversations.map(conv -> {
            // Get last message preview
            List<ChatMessage> lastMessages = chatMessageRepository
                    .findByConversationIdOrderByCreatedAtDesc(conv.getId(), PageRequest.of(0, 1));
            String lastMessage = lastMessages.isEmpty() ? null :
                    truncate(lastMessages.getFirst().getContent(), 100);

            long messageCount = chatMessageRepository.countByConversationId(conv.getId());

            return ConversationDto.builder()
                    .id(conv.getId())
                    .title(conv.getTitle())
                    .lastMessage(lastMessage)
                    .messageCount(messageCount)
                    .createdAt(conv.getCreatedAt())
                    .updatedAt(conv.getUpdatedAt())
                    .build();
        });
    }

    /**
     * Get a single conversation with all messages.
     */
    @Transactional(readOnly = true)
    public Conversation getConversationWithMessages(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndUserIdWithMessages(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
    }

    /**
     * Update conversation title.
     */
    @Transactional
    public ConversationDto updateTitle(Long conversationId, Long userId, String title) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        conversation.setTitle(title);
        conversation = conversationRepository.save(conversation);

        return ConversationDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    /**
     * Delete a conversation and all its messages.
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        conversationRepository.delete(conversation);
        log.info("Deleted conversation {} for user {}", conversationId, userId);
    }

    /**
     * Auto-generate a title from the first user message.
     * Runs asynchronously to avoid blocking the chat response.
     */
    @Async
    @Transactional
    public void autoGenerateTitle(Long conversationId, String firstMessage) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            if ("New Chat".equals(conv.getTitle())) {
                String title = truncate(firstMessage, 50);
                conv.setTitle(title);
                conversationRepository.save(conv);
                log.debug("Auto-generated title for conversation {}: '{}'", conversationId, title);
            }
        });
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
