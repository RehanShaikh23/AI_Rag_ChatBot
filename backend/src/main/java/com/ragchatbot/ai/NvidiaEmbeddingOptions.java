package com.ragchatbot.ai;

import org.springframework.ai.embedding.EmbeddingOptions;

/**
 * Carries the NVIDIA-specific {@code input_type} parameter.
 * <p>
 * NVIDIA's asymmetric embedding models (e.g. nv-embedqa-e5-v5) require
 * different input types for documents vs queries:
 * <ul>
 *   <li>{@code "passage"} — when embedding documents for storage</li>
 *   <li>{@code "query"}   — when embedding user questions for search</li>
 * </ul>
 */
public class NvidiaEmbeddingOptions implements EmbeddingOptions {

    private final String inputType;

    public NvidiaEmbeddingOptions(String inputType) {
        this.inputType = inputType;
    }

    public String getInputType() {
        return inputType;
    }

    @Override
    public Integer getDimensions() {
        // Return null to use the model's default dimensions
        return null;
    }

    @Override
    public String getModel() {
        // Return null — the model is set on the NvidiaEmbeddingModel itself
        return null;
    }

    /** Use when embedding documents/passages for storage in the vector store. */
    public static final NvidiaEmbeddingOptions PASSAGE = new NvidiaEmbeddingOptions("passage");

    /** Use when embedding user queries for similarity search. */
    public static final NvidiaEmbeddingOptions QUERY = new NvidiaEmbeddingOptions("query");
}
