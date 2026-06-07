package com.ragchatbot.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) service.
 * Handles document ingestion (parse → chunk → embed → store)
 * and similarity search for context retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;

    @Value("${app.rag.chunk-size:800}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${app.rag.max-results:5}")
    private int maxResults;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    /**
     * Ingest a document: parse → split into chunks → embed → store in vector DB.
     *
     * @param resource The file resource to process
     * @param userId   The owner user ID (stored as metadata)
     * @param docId    The document entity ID (stored as metadata)
     * @return The number of chunks created
     */
    public int ingestDocument(Resource resource, Long userId, Long docId) {
        log.info("Ingesting document: {} for user {}", resource.getFilename(), userId);

        // 1. Parse the document using Apache Tika
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> rawDocs = reader.get();
        log.debug("Parsed {} raw document sections", rawDocs.size());

        // 2. Split into manageable chunks
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        List<Document> chunks = splitter.apply(rawDocs);
        log.debug("Split into {} chunks", chunks.size());

        // 3. Add metadata to each chunk
        chunks.forEach(chunk -> {
            chunk.getMetadata().put("userId", userId.toString());
            chunk.getMetadata().put("documentId", docId.toString());
            chunk.getMetadata().put("fileName", resource.getFilename());
        });

        // 4. Embed and store in vector DB
        vectorStore.add(chunks);
        log.info("Stored {} chunks in vector store for document {}", chunks.size(), docId);

        return chunks.size();
    }

    /**
     * Search the vector store for chunks relevant to the user's query.
     *
     * @param query  The user's question
     * @param userId The user ID to scope the search
     * @return Concatenated relevant text, or null if no relevant matches
     */
    public String searchRelevantContext(String query, Long userId) {
        log.debug("RAG search — query: '{}', user: {}", query.substring(0, Math.min(query.length(), 50)), userId);

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(maxResults)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression("userId == '" + userId + "'")
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            if (results == null || results.isEmpty()) {
                log.debug("No relevant RAG results found");
                return null;
            }

            log.debug("Found {} relevant chunks", results.size());

            return results.stream()
                    .map(doc -> {
                        String source = (String) doc.getMetadata().getOrDefault("fileName", "unknown");
                        return "[Source: " + source + "]\n" + doc.getText();
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));
        } catch (Exception e) {
            log.warn("RAG search failed (vector store may not be ready): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Delete all vector store entries for a specific document.
     */
    public void deleteDocumentChunks(Long docId) {
        try {
            // Spring AI VectorStore doesn't have a direct delete-by-metadata,
            // so we search and delete by IDs
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("*")
                    .topK(10000)
                    .filterExpression("documentId == '" + docId + "'")
                    .build();

            List<Document> docs = vectorStore.similaritySearch(searchRequest);
            if (docs != null && !docs.isEmpty()) {
                List<String> ids = docs.stream().map(Document::getId).collect(Collectors.toList());
                vectorStore.delete(ids);
                log.info("Deleted {} vector chunks for document {}", ids.size(), docId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete document chunks: {}", e.getMessage());
        }
    }
}
