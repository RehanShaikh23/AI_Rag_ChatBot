package com.ragchatbot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) service.
 * Handles document ingestion (parse → chunk → embed → store)
 * and similarity search for context retrieval.
 */
@Service
@Slf4j
public class RagService {

    private final VectorStore vectorStore;

    @Value("${app.rag.chunk-size:800}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${app.rag.max-results:10}")
    private int maxResults;

    @Value("${app.rag.similarity-threshold:0.1}")
    private double similarityThreshold;

    @Value("${app.rag.max-embedding-tokens:512}")
    private int maxEmbeddingTokens;

    @Value("${app.rag.full-doc-chunk-threshold:15}")
    private int fullDocChunkThreshold;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

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

        if (rawDocs.isEmpty()) {
            log.warn("⚠️ Tika parsed 0 sections from '{}' — file may be empty or unsupported",
                    resource.getFilename());
            return 0;
        }

        // Log a preview of the parsed content to verify Tika is reading correctly
        String preview = rawDocs.get(0).getText();
        log.info("📄 Parsed content preview (first 200 chars): {}",
                preview.substring(0, Math.min(preview.length(), 200)));

        // 2. Split into manageable chunks
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        List<Document> chunks = splitter.apply(rawDocs);
        log.info("✂️ Split into {} chunks (chunkSize={}, overlap={})", chunks.size(), chunkSize, chunkOverlap);

        // 2b. Safety net — truncate any chunk that exceeds the embedding model's token limit.
        //     TokenTextSplitter can occasionally produce slightly oversized chunks at boundaries.
        for (Document chunk : chunks) {
            String text = chunk.getText();
            // Rough token estimate: ~4 chars per token for English text.
            // If the chunk is suspiciously long, hard-truncate to be safe.
            int estimatedTokens = text.length() / 4;
            if (estimatedTokens > maxEmbeddingTokens) {
                int safeCharLimit = maxEmbeddingTokens * 4;
                // Truncate at the last space before the limit to avoid splitting mid-word
                int truncateAt = text.lastIndexOf(' ', safeCharLimit);
                if (truncateAt <= 0) truncateAt = safeCharLimit;
                String truncated = text.substring(0, truncateAt);
                log.warn("⚠️ Chunk exceeded {} estimated tokens ({} chars) — truncated to {} chars",
                        maxEmbeddingTokens, text.length(), truncated.length());
                chunk.getMetadata().put("truncated", "true");
                // Create a new document with truncated text but same metadata
                chunks.set(chunks.indexOf(chunk),
                        new Document(chunk.getId(), truncated, chunk.getMetadata()));
            }
        }

        // 3. Add metadata to each chunk
        chunks.forEach(chunk -> {
            chunk.getMetadata().put("userId", userId.toString());
            chunk.getMetadata().put("documentId", docId.toString());
            chunk.getMetadata().put("fileName", resource.getFilename());
        });

        // 4. Store in vector DB — NvidiaEmbeddingModel.embed(Document) automatically
        //    uses input_type="passage" for correct asymmetric embedding.
        vectorStore.add(chunks);
        log.info("✅ Stored {} chunks in vector store for document {} (user {})",
                chunks.size(), docId, userId);

        return chunks.size();
    }

    /**
     * Retrieve the FULL text of a document by concatenating all its chunks.
     * Used for small documents (e.g. CVs) where we want the AI to see everything
     * rather than relying on similarity search which may miss key details.
     * <p>
     * When PGVector is available, uses native filter expressions to push filtering
     * to the database. Falls back to in-memory filtering for SimpleVectorStore.
     *
     * @param docId  The document ID to retrieve
     * @param userId The user ID (for ownership verification in post-filter)
     * @return Full document text, or null if no chunks found
     */
    public String getFullDocumentText(Long docId, Long userId) {
        log.info("📄 Fetching FULL document text for doc {} (user {})", docId, userId);

        try {
            boolean isSimpleStore = vectorStore instanceof SimpleVectorStore;

            SearchRequest.Builder builder = SearchRequest.builder()
                    .query("document content")  // Neutral query for broad match
                    .topK(isSimpleStore ? 10000 : 500)  // PGVector: reasonable limit; Simple: fetch all
                    .similarityThreshold(0.0);

            // PGVector supports native filter expressions — push filtering to the DB
            if (!isSimpleStore) {
                builder.filterExpression(
                        "userId == '" + userId + "' && documentId == '" + docId + "'");
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ No chunks found in vector store for doc {} (user {})", docId, userId);
                return null;
            }

            // In-memory post-filter only needed for SimpleVectorStore
            List<Document> docChunks;
            if (isSimpleStore) {
                docChunks = results.stream()
                        .filter(d -> docId.toString().equals(d.getMetadata().get("documentId")))
                        .filter(d -> userId.toString().equals(d.getMetadata().get("userId")))
                        .collect(Collectors.toList());
            } else {
                docChunks = results;
            }

            if (docChunks.isEmpty()) {
                log.warn("⚠️ No chunks found for document {} (user {})", docId, userId);
                return null;
            }

            String fullText = docChunks.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));

            String source = (String) docChunks.get(0).getMetadata().getOrDefault("fileName", "unknown");
            log.info("✅ Retrieved {} chunks for full document text (source: '{}', total length: {})",
                    docChunks.size(), source, fullText.length());

            return "[Source: " + source + "]\n" + fullText;
        } catch (Exception e) {
            log.error("❌ Failed to fetch full document text: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Search the vector store for chunks relevant to the user's query,
     * scoped to a specific document.
     *
     * @param query      The user's question
     * @param userId     The user ID to scope the search
     * @param documentId The document ID to scope the search (may be null to search all)
     * @return Concatenated relevant text, or null if no relevant matches
     */
    public String searchRelevantContext(String query, Long userId, Long documentId) {
        String queryPreview = query.substring(0, Math.min(query.length(), 80));
        log.info("🔍 RAG search — query: '{}', user: {}, docId: {}", queryPreview, userId, documentId);

        try {
            // For SimpleVectorStore we fetch more results and filter in-memory,
            // because it doesn't support native filter expressions.
            boolean isSimpleStore = vectorStore instanceof SimpleVectorStore;
            int fetchTopK = isSimpleStore ? maxResults * 5 : maxResults;
            double fetchThreshold = isSimpleStore ? 0.0 : similarityThreshold;

            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(fetchTopK)
                    .similarityThreshold(fetchThreshold);

            // PGVector supports native filter expressions
            if (!isSimpleStore) {
                if (documentId != null) {
                    builder.filterExpression(
                            "userId == '" + userId + "' && documentId == '" + documentId + "'");
                } else {
                    builder.filterExpression("userId == '" + userId + "'");
                }
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ RAG search returned 0 results for query: '{}' (threshold: {})",
                        queryPreview, similarityThreshold);
                return null;
            }

            // In-memory post-filtering for SimpleVectorStore
            if (isSimpleStore) {
                results = results.stream()
                        .filter(d -> userId.toString().equals(d.getMetadata().get("userId")))
                        .filter(d -> documentId == null ||
                                documentId.toString().equals(d.getMetadata().get("documentId")))
                        .limit(maxResults)
                        .collect(Collectors.toList());

                if (results.isEmpty()) {
                    log.warn("⚠️ In-memory filter removed all results (userId: {}, docId: {})",
                            userId, documentId);
                    return null;
                }
            }

            log.info("✅ RAG found {} relevant chunks", results.size());
            results.forEach(doc -> {
                String fileName = (String) doc.getMetadata().getOrDefault("fileName", "unknown");
                String docIdMeta = (String) doc.getMetadata().getOrDefault("documentId", "?");
                String textPreview = doc.getText().substring(0, Math.min(doc.getText().length(), 120));
                log.info("  → source: '{}' (docId: {}), preview: {}...", fileName, docIdMeta, textPreview);
            });

            return results.stream()
                    .map(doc -> {
                        String source = (String) doc.getMetadata().getOrDefault("fileName", "unknown");
                        return "[Source: " + source + "]\n" + doc.getText();
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));
        } catch (Exception e) {
            log.error("❌ RAG search FAILED — AI will respond WITHOUT document context. Error: {}",
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * Search the vector store for chunks relevant to the user's query.
     * Searches across all documents for the user.
     *
     * @param query  The user's question
     * @param userId The user ID to scope the search
     * @return Concatenated relevant text, or null if no relevant matches
     */
    public String searchRelevantContext(String query, Long userId) {
        return searchRelevantContext(query, userId, null);
    }

    /**
     * Delete all vector store entries for a specific document.
     */
    public void deleteDocumentChunks(Long docId) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("*")
                    .topK(10000)
                    .build();

            List<Document> docs = vectorStore.similaritySearch(searchRequest);
            if (docs != null && !docs.isEmpty()) {
                // Filter by documentId in-memory (works with any vector store)
                List<String> ids = docs.stream()
                        .filter(d -> docId.toString().equals(d.getMetadata().get("documentId")))
                        .map(Document::getId)
                        .collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    vectorStore.delete(ids);
                    log.info("Deleted {} vector chunks for document {}", ids.size(), docId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to delete document chunks: {}", e.getMessage());
        }
    }

    /**
     * @return The configured threshold below which docs are sent in full.
     */
    public int getFullDocChunkThreshold() {
        return fullDocChunkThreshold;
    }
}
