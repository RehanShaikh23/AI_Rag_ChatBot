package com.ragchatbot.ai;

import com.ragchatbot.entity.Document;
import com.ragchatbot.entity.Document.DocumentStatus;
import com.ragchatbot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles async document processing in a separate bean so that
 * Spring's AOP proxy correctly applies both @Async and @Transactional.
 * <p>
 * If this logic were in the same class that calls it (DocumentService),
 * the self-call would bypass the proxy and neither annotation would work.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessor {

    private final RagService ragService;
    private final DocumentRepository documentRepository;

    /**
     * Asynchronously process a document: parse → chunk → embed → store.
     * Runs in a separate thread pool managed by Spring's @Async.
     */
    @Async
    @Transactional
    public void processAsync(Long documentId, byte[] fileBytes, String fileName, Long userId) {
        log.info("🔄 Async processing document {} in thread {}", documentId, Thread.currentThread().getName());

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.saveAndFlush(document);

            Resource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            int chunkCount = ragService.ingestDocument(resource, userId, documentId);

            document.setChunkCount(chunkCount);
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.saveAndFlush(document);

            log.info("✅ Document {} processed — {} chunks stored", documentId, chunkCount);
        } catch (Exception e) {
            log.error("❌ Document {} processing failed: {}", documentId, e.getMessage(), e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.saveAndFlush(document);
        }
    }
}
