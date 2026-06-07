package com.ragchatbot.service;

import com.ragchatbot.ai.RagService;
import com.ragchatbot.dto.document.DocumentDto;
import com.ragchatbot.entity.Document;
import com.ragchatbot.entity.Document.DocumentStatus;
import com.ragchatbot.entity.User;
import com.ragchatbot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
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

        // Process asynchronously
        byte[] fileBytes = file.getBytes();
        processDocumentAsync(document.getId(), fileBytes, file.getOriginalFilename(), user.getId());

        return toDto(document);
    }

    /**
     * Async document processing: parse → chunk → embed → store.
     */
    @Async
    @Transactional
    public void processDocumentAsync(Long documentId, byte[] fileBytes, String fileName, Long userId) {
        log.info("Processing document {} asynchronously", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            Resource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            int chunkCount = ragService.ingestDocument(resource, userId, documentId);

            document.setChunkCount(chunkCount);
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);

            log.info("Document {} processed successfully — {} chunks", documentId, chunkCount);
        } catch (Exception e) {
            log.error("Document processing failed for {}: {}", documentId, e.getMessage(), e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
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
