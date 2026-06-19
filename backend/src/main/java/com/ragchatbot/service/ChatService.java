package com.ragchatbot.service;

import com.ragchatbot.ai.AiChatService;
import com.ragchatbot.ai.RagService;
import com.ragchatbot.dto.chat.ChatRequest;
import com.ragchatbot.dto.chat.ChatResponse;
import com.ragchatbot.entity.ChatMessage;
import com.ragchatbot.entity.ChatMessage.MessageRole;
import com.ragchatbot.entity.Conversation;
import com.ragchatbot.entity.Document;
import com.ragchatbot.entity.Document.DocumentStatus;
import com.ragchatbot.entity.User;
import com.ragchatbot.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the full chat flow:
 * save user message → RAG lookup → AI call → save response → return.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final AiChatService aiChatService;
    private final RagService ragService;
    private final ConversationService conversationService;
    private final AuthService authService;
    private final DocumentService documentService;
    private final ChatMessageRepository chatMessageRepository;

    private static final int MAX_CONTEXT_MESSAGES = 10;

    // Regex to detect code blocks in AI responses: ```lang\n...\n```
    private static final Pattern CODE_BLOCK_PATTERN =
            Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```", Pattern.MULTILINE);

    /**
     * Resolve the active document ID for RAG:
     *  - If the request specifies one, use it.
     *  - Otherwise, fall back to the user's most recently uploaded document.
     */
    private Long resolveActiveDocumentId(ChatRequest request, Long userId) {
        if (request.getActiveDocumentId() != null) {
            log.debug("Using client-specified activeDocumentId: {}", request.getActiveDocumentId());
            return request.getActiveDocumentId();
        }
        Long latestId = documentService.getLatestDocumentId(userId);
        log.debug("No activeDocumentId in request — resolved latest: {}", latestId);
        return latestId;
    }

    /**
     * Fetch RAG context, preferring full-document retrieval for small docs.
     * Checks document processing status first to prevent hallucination on failed documents.
     */
    private String fetchRagContext(String message, Long userId, Long documentId) {
        if (documentId == null) {
            log.info("📭 No documents available — RAG context will be null");
            return null;
        }

        // Check document processing status before attempting RAG lookup
        Document doc = documentService.getDocumentEntity(documentId);
        if (doc != null && doc.getStatus() == DocumentStatus.FAILED) {
            log.warn("❌ Document {} has FAILED status — injecting error context", documentId);
            String errorDetail = doc.getErrorMessage() != null ? doc.getErrorMessage() : "Unknown processing error";
            return "[DOCUMENT_ERROR] The document '" + doc.getFileName()
                    + "' failed to process and could not be indexed for search. "
                    + "Error: " + errorDetail
                    + ". The user should be informed that their document could not be processed.";
        }
        if (doc != null && doc.getStatus() != DocumentStatus.COMPLETED) {
            log.info("⏳ Document {} is still {} — injecting processing context", documentId, doc.getStatus());
            return "[DOCUMENT_PROCESSING] The document '" + doc.getFileName()
                    + "' is still being processed (status: " + doc.getStatus()
                    + "). The user should be asked to wait a moment and try again.";
        }

        int chunkCount = documentService.getDocumentChunkCount(documentId);
        int threshold = ragService.getFullDocChunkThreshold();

        if (chunkCount > 0 && chunkCount <= threshold) {
            // Small document → send the full text so nothing is missed
            log.info("📄 Document {} has {} chunks (≤ threshold {}) — using FULL document text",
                    documentId, chunkCount, threshold);
            String fullText = ragService.getFullDocumentText(documentId, userId);
            if (fullText != null) {
                return fullText;
            }
            // Fall through to similarity search if full-text fetch fails
            log.warn("⚠️ Full-text fetch returned null — falling back to similarity search");
        }

        // Large document or fallback → use similarity search scoped to this document
        return ragService.searchRelevantContext(message, userId, documentId);
    }

    /**
     * Send a message synchronously and get the full AI response.
     */
    @Transactional
    public ChatResponse sendMessage(ChatRequest request, String userEmail) {
        log.info("Chat request from {} — conversationId: {}", userEmail, request.getConversationId());

        User user = authService.getUserByEmail(userEmail);

        // 1. Get or create conversation
        Conversation conversation;
        boolean isNewConversation = false;
        if (request.getConversationId() != null) {
            conversation = conversationService.getConversationWithMessages(
                    request.getConversationId(), user.getId());
        } else {
            conversation = conversationService.createConversation(user, null);
            isNewConversation = true;
        }

        // 2. Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .build();
        userMsg = chatMessageRepository.save(userMsg);

        // 3. Auto-generate title from first message
        if (isNewConversation) {
            conversationService.autoGenerateTitle(conversation.getId(), request.getMessage());
        }

        // 4. Resolve active document + build context IN PARALLEL
        Long activeDocId = resolveActiveDocumentId(request, user.getId());

        CompletableFuture<List<ChatMessage>> historyFuture = CompletableFuture.supplyAsync(() ->
                chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()));

        CompletableFuture<String> ragFuture = CompletableFuture.supplyAsync(() ->
                request.isUseRag()
                        ? fetchRagContext(request.getMessage(), user.getId(), activeDocId)
                        : null);

        // Wait for both to complete
        List<ChatMessage> historyEntities = historyFuture.join();
        String ragContext = ragFuture.join();
        boolean ragUsed = ragContext != null;

        List<ChatMessage> contextHistory = historyEntities.size() > 1
                ? historyEntities.subList(0, historyEntities.size() - 1)
                : List.of();
        List<Message> aiHistory = aiChatService.buildMessageHistory(contextHistory, MAX_CONTEXT_MESSAGES);

        // 6. Call AI
        String aiResponseText = aiChatService.chat(request.getMessage(), aiHistory, ragContext);

        // 7. Extract code block (if present)
        ChatResponse.CodeBlock codeBlock = extractCodeBlock(aiResponseText);
        String cleanedContent = codeBlock != null
                ? aiResponseText.replaceFirst(CODE_BLOCK_PATTERN.pattern(), "").trim()
                : aiResponseText;

        // 8. Save assistant message
        ChatMessage botMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(aiResponseText)
                .codeBlock(codeBlock != null ? serializeCodeBlock(codeBlock) : null)
                .build();
        botMsg = chatMessageRepository.save(botMsg);

        log.info("Chat response saved — conversationId: {}, msgId: {}", conversation.getId(), botMsg.getId());

        // Reload conversation for updated title
        String title = conversation.getTitle();
        if (isNewConversation) {
            Conversation updated = conversationService.getConversationWithMessages(conversation.getId(), user.getId());
            title = updated.getTitle();
        }

        return ChatResponse.builder()
                .messageId(botMsg.getId())
                .conversationId(conversation.getId())
                .conversationTitle(title)
                .role("ASSISTANT")
                .content(aiResponseText)
                .codeBlock(codeBlock)
                .ragUsed(ragUsed)
                .createdAt(botMsg.getCreatedAt())
                .build();
    }

    /**
     * Stream a response via SSE. The user message is saved before streaming;
     * the assistant message is saved after the stream completes.
     * NOTE: NOT @Transactional — the Flux is consumed AFTER this method returns,
     * so a transaction here would close prematurely. Each DB operation manages
     * its own transaction instead.
     */
    public StreamContext prepareStream(ChatRequest request, String userEmail) {
        User user = authService.getUserByEmail(userEmail);

        Conversation conversation;
        boolean isNewConversation = false;
        if (request.getConversationId() != null) {
            conversation = conversationService.getConversationWithMessages(
                    request.getConversationId(), user.getId());
        } else {
            conversation = conversationService.createConversation(user, null);
            isNewConversation = true;
        }

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMsg);

        if (isNewConversation) {
            conversationService.autoGenerateTitle(conversation.getId(), request.getMessage());
        }

        // Resolve active document + build context IN PARALLEL
        Long activeDocId = resolveActiveDocumentId(request, user.getId());

        CompletableFuture<List<ChatMessage>> historyFuture = CompletableFuture.supplyAsync(() ->
                chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()));

        CompletableFuture<String> ragFuture = CompletableFuture.supplyAsync(() ->
                request.isUseRag()
                        ? fetchRagContext(request.getMessage(), user.getId(), activeDocId)
                        : null);

        List<ChatMessage> historyEntities = historyFuture.join();
        String ragContext = ragFuture.join();

        List<ChatMessage> contextHistory = historyEntities.size() > 1
                ? historyEntities.subList(0, historyEntities.size() - 1)
                : List.of();
        List<Message> aiHistory = aiChatService.buildMessageHistory(contextHistory, MAX_CONTEXT_MESSAGES);

        Flux<String> stream = aiChatService.streamChat(request.getMessage(), aiHistory, ragContext);

        return new StreamContext(conversation.getId(), user.getId(), stream);
    }

    /**
     * Save the completed streamed response.
     */
    @Transactional
    public void saveStreamedResponse(Long conversationId, String fullContent) {
        ChatMessage botMsg = ChatMessage.builder()
                .conversation(Conversation.builder().id(conversationId).build())
                .role(MessageRole.ASSISTANT)
                .content(fullContent)
                .codeBlock(extractCodeBlock(fullContent) != null ? serializeCodeBlock(extractCodeBlock(fullContent)) : null)
                .build();
        chatMessageRepository.save(botMsg);
        log.debug("Saved streamed response for conversation {}", conversationId);
    }

    /**
     * Get all messages for a conversation.
     */
    @Transactional(readOnly = true)
    public List<ChatResponse> getMessages(Long conversationId, String userEmail) {
        User user = authService.getUserByEmail(userEmail);
        // Verify ownership
        conversationService.getConversationWithMessages(conversationId, user.getId());

        List<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        return messages.stream().map(msg -> {
            ChatResponse.CodeBlock codeBlock = null;
            if (msg.getCodeBlock() != null) {
                codeBlock = deserializeCodeBlock(msg.getCodeBlock());
            } else {
                codeBlock = extractCodeBlock(msg.getContent());
            }

            return ChatResponse.builder()
                    .messageId(msg.getId())
                    .conversationId(conversationId)
                    .role(msg.getRole().name())
                    .content(msg.getContent())
                    .codeBlock(codeBlock)
                    .tokenCount(msg.getTokenCount())
                    .createdAt(msg.getCreatedAt())
                    .build();
        }).toList();
    }

    /**
     * Extract the first code block from an AI response.
     */
    private ChatResponse.CodeBlock extractCodeBlock(String text) {
        if (text == null) return null;
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            String lang = matcher.group(1) != null ? matcher.group(1) : "text";
            String code = matcher.group(2).trim();
            return ChatResponse.CodeBlock.builder().lang(lang).content(code).build();
        }
        return null;
    }

    private String serializeCodeBlock(ChatResponse.CodeBlock cb) {
        return "{\"lang\":\"" + cb.getLang() + "\",\"content\":\"" +
                cb.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
    }

    private ChatResponse.CodeBlock deserializeCodeBlock(String json) {
        try {
            // Simple JSON parse for code block
            int langStart = json.indexOf("\"lang\":\"") + 8;
            int langEnd = json.indexOf("\"", langStart);
            int contentStart = json.indexOf("\"content\":\"") + 11;
            int contentEnd = json.lastIndexOf("\"");
            if (langStart > 7 && contentStart > 10) {
                String lang = json.substring(langStart, langEnd);
                String content = json.substring(contentStart, contentEnd)
                        .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                return ChatResponse.CodeBlock.builder().lang(lang).content(content).build();
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize code block: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Holds stream context for SSE delivery.
     */
    public record StreamContext(Long conversationId, Long userId, Flux<String> stream) {}
}
