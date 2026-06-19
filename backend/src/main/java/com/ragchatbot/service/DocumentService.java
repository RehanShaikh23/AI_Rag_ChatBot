package com.ragchatbot.service;

import com.ragchatbot.ai.DocumentProcessor;
import com.ragchatbot.ai.RagService;
import com.ragchatbot.dto.document.DocumentDto;
import com.ragchatbot.entity.Document;
import com.ragchatbot.entity.Document.DocumentStatus;
import com.ragchatbot.entity.User;
import com.ragchatbot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Handles document upload, async RAG processing, and listing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final RagService ragService;
    private final AuthService authService;
    private final DocumentProcessor documentProcessor;

    /**
     * Upload and process a document for RAG.
     */
    @Transactional
    public DocumentDto uploadDocument(MultipartFile file, String userEmail) throws IOException {
        User user = authService.getUserByEmail(userEmail);

        log.info("Uploading document: {} ({} bytes) for user {}",
                file.getOriginalFilename(), file.getSize(), user.getId());

        Document document = Document.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .status(DocumentStatus.PENDING)
                .build();
        document = documentRepository.save(document);

        // Capture bytes before the MultipartFile is closed, then
        // delegate to a separate bean so @Async + @Transactional work correctly.
        byte[] fileBytes = file.getBytes();
        documentProcessor.processAsync(document.getId(), fileBytes, file.getOriginalFilename(), user.getId());

        return toDto(document);
    }

    /**
     * List documents for a user.
     */
    @Transactional(readOnly = true)
    public Page<DocumentDto> getDocuments(String userEmail, int page, int size) {
        User user = authService.getUserByEmail(userEmail);
        return documentRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(this::toDto);
    }

    /**
     * Delete a document and its vector store entries.
     */
    @Transactional
    public void deleteDocument(Long documentId, String userEmail) {
        User user = authService.getUserByEmail(userEmail);
        Document document = documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        ragService.deleteDocumentChunks(documentId);
        documentRepository.delete(document);
        log.info("Deleted document {} for user {}", documentId, user.getId());
    }

    /**
     * Get the ID of the user's most recently uploaded (completed) document.
     * Returns null if no completed documents exist.
     */
    @Transactional(readOnly = true)
    public Long getLatestDocumentId(Long userId) {
        return documentRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, Document.DocumentStatus.COMPLETED)
                .map(Document::getId)
                .orElse(null);
    }

    /**
     * Get the chunk count for a specific document.
     * Returns 0 if document not found.
     */
    @Transactional(readOnly = true)
    public int getDocumentChunkCount(Long documentId) {
        return documentRepository.findById(documentId)
                .map(doc -> doc.getChunkCount() != null ? doc.getChunkCount() : 0)
                .orElse(0);
    }

    /**
     * Get a document entity by ID (for status checks).
     * Returns null if not found.
     */
    @Transactional(readOnly = true)
    public Document getDocumentEntity(Long documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

    private DocumentDto toDto(Document doc) {
        return DocumentDto.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .chunkCount(doc.getChunkCount())
                .status(doc.getStatus().name())
                .errorMessage(doc.getErrorMessage())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}

