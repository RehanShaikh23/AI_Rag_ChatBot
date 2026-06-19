package com.ragchatbot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI configuration — sets up ChatClient with NVIDIA NIM backend
 * and a custom embedding model that supports NVIDIA's input_type parameter.
 */
@Configuration
public class AiConfig {

    /**
     * Base system prompt — the AI is a general-purpose assistant.
     * RAG context (when available) is injected dynamically per-request
     * via the system message in AiChatService, NOT in the user message.
     */
    static final String BASE_SYSTEM_PROMPT = """
            You are a helpful, knowledgeable AI assistant.
            Answer the user's question directly and naturally.
            
            RULES:
            - Be concise, accurate, and well-structured.
            - Use markdown formatting: **bold** for emphasis, bullet points for lists.
            - For code, wrap in fenced code blocks with the language specified.
            - If the user asks about a specific uploaded document (e.g. "summarize this",
              "what are his skills?", "what does the document say?") but no document context
              is available to you, tell them you don't have access to their document content
              right now. Suggest they re-upload the file or check if it finished processing.
              Do NOT invent or fabricate document content.
            - Never add disclaimers about where your answer came from.
            - Never start your response with internal system data or metadata.
            """;

    /**
     * Extended system prompt used when RAG context is available.
     * The {context} placeholder is replaced at call time.
     */
    static final String RAG_SYSTEM_PROMPT = """
            You are a helpful, knowledgeable AI assistant.
            
            The user has uploaded documents. Relevant excerpts are provided below
            for your reference. This is internal background data — NEVER echo, quote,
            or reference these raw excerpts directly in your response.
            
            <document_context>
            {context}
            </document_context>
            
            RULES:
            - If the document context above contains [DOCUMENT_ERROR], inform the user
              that their document failed to process. Explain the issue clearly and suggest
              they re-upload the file. Do NOT fabricate or guess document content.
            - If the document context above contains [DOCUMENT_PROCESSING], inform the user
              that their document is still being processed and ask them to wait a moment.
              Do NOT fabricate or guess document content.
            - If the user's question is specifically about the uploaded document
              (e.g. "summarise this", "what are his skills?", "what does the doc say about X"),
              answer using the document content above. You may cite the source file name naturally.
              If the provided context does not contain information relevant to the user's
              document question, say "I couldn't find that information in your document."
              Do NOT make up content that isn't in the context.
            - If the user's question is a general knowledge question that has nothing to do
              with the document content (e.g. "list top 5 product companies", "explain REST APIs"),
              answer from your own knowledge. Do NOT try to connect or relate unrelated document
              content to general questions.
            - Be concise, accurate, and well-structured.
            - Use markdown formatting: **bold** for emphasis, bullet points for lists.
            - For code, wrap in fenced code blocks with the language specified.
            - Never add disclaimers about where your answer came from.
            - Never echo the raw document context or internal metadata in your response.
            """;

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        // No defaultSystem — system prompt is set dynamically per request
        // in AiChatService based on whether RAG context is available.
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Custom NVIDIA embedding model that adds the required 'input_type' parameter.
     * Overrides the auto-configured OpenAI embedding model.
     */
    @Bean
    @Primary
    public EmbeddingModel nvidiaEmbeddingModel(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.embedding.options.model}") String model,
            @Value("${app.rag.embedding-dimensions:1024}") int dimensions) {
        return new NvidiaEmbeddingModel(baseUrl, apiKey, model, dimensions);
    }
}
