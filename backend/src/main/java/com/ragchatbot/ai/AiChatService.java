package com.ragchatbot.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Core AI chat service that wraps Spring AI's ChatClient for
 * synchronous and streaming responses with conversation context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ChatClient chatClient;

    /**
     * Send a prompt with conversation history and get a complete response.
     *
     * @param prompt              The user's message
     * @param conversationHistory Previous messages for context
     * @param ragContext          Optional RAG-retrieved context (null if not used)
     * @return The AI-generated response text
     */
    public String chat(String prompt, List<Message> conversationHistory, String ragContext) {
        log.debug("AI chat request — prompt length: {}, history size: {}, RAG context: {}",
                prompt.length(), conversationHistory.size(), ragContext != null ? "yes" : "no");

        String systemPrompt = getSystemPrompt(ragContext);

        try {
            long startTime = System.currentTimeMillis();
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(conversationHistory)
                    .user(prompt)  // raw user question — never modified
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("⚡ AI response time: {}ms — length: {}", elapsed, response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get AI response: " + e.getMessage(), e);
        }
    }

    /**
     * Stream a response token-by-token via Flux for SSE delivery.
     *
     * @param prompt              The user's message
     * @param conversationHistory Previous messages for context
     * @param ragContext          Optional RAG-retrieved context
     * @return A Flux of string tokens
     */
    public Flux<String> streamChat(String prompt, List<Message> conversationHistory, String ragContext) {
        log.debug("AI stream request — prompt length: {}, history size: {}, RAG context: {}",
                prompt.length(), conversationHistory.size(), ragContext != null ? "yes" : "no");

        String systemPrompt = getSystemPrompt(ragContext);

        try {
            long startTime = System.currentTimeMillis();
            java.util.concurrent.atomic.AtomicBoolean firstToken = new java.util.concurrent.atomic.AtomicBoolean(true);
            return chatClient.prompt()
                    .system(systemPrompt)
                    .messages(conversationHistory)
                    .user(prompt)  // raw user question — never modified
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        if (firstToken.compareAndSet(true, false)) {
                            long ttft = System.currentTimeMillis() - startTime;
                            log.info("⚡ Stream time-to-first-token: {}ms", ttft);
                        }
                    });
        } catch (Exception e) {
            log.error("AI stream error: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("Failed to stream AI response: " + e.getMessage(), e));
        }
    }

    /**
     * Convert stored message entities to Spring AI Message objects.
     */
    public List<Message> buildMessageHistory(
            List<com.ragchatbot.entity.ChatMessage> storedMessages, int maxMessages) {

        List<Message> messages = new ArrayList<>();
        int start = Math.max(0, storedMessages.size() - maxMessages);

        for (int i = start; i < storedMessages.size(); i++) {
            com.ragchatbot.entity.ChatMessage msg = storedMessages.get(i);
            switch (msg.getRole()) {
                case USER -> messages.add(new UserMessage(msg.getContent()));
                case ASSISTANT -> messages.add(new AssistantMessage(msg.getContent()));
                default -> { /* skip SYSTEM messages in history */ }
            }
        }
        return messages;
    }

    /**
     * Select the appropriate system prompt based on whether RAG context exists.
     * When context is available, it is embedded inside the system prompt
     * (hidden from the user) — NOT in the user message.
     */
    private String getSystemPrompt(String ragContext) {
        if (ragContext == null || ragContext.isBlank()) {
            // No documents or no relevant results — use base prompt
            return AiConfig.BASE_SYSTEM_PROMPT;
        }
        // Inject document context into the RAG system prompt
        return AiConfig.RAG_SYSTEM_PROMPT.replace("{context}", ragContext);
    }
}
