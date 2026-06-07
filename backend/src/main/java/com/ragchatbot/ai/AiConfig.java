package com.ragchatbot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration — sets up ChatClient with NVIDIA NIM backend.
 */
@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are an advanced AI assistant powered by RAG (Retrieval-Augmented Generation).
            You provide accurate, helpful, and well-structured responses.
            
            Guidelines:
            - Be concise but thorough.
            - When providing code, always specify the language and ensure it is correct and runnable.
            - Format code blocks clearly. If you include code, wrap it in a code block with the language specified.
            - Use markdown-style formatting: **bold** for emphasis, bullet points for lists.
            - When RAG context is provided, base your answer on that context first, then supplement with your knowledge.
            - If you don't know something, say so rather than making up information.
            - Be professional and helpful.
            """;

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
