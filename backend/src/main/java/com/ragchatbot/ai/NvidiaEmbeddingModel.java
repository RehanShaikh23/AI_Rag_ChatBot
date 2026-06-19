package com.ragchatbot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom EmbeddingModel for NVIDIA NIM's embedding API.
 * <p>
 * NVIDIA's asymmetric embedding models (e.g., nv-embedqa-e5-v5) require
 * an {@code input_type} parameter ("query" or "passage") that Spring AI's
 * built-in OpenAI client doesn't send. This wrapper adds it.
 * <p>
 * The input_type is resolved automatically:
 * <ul>
 *   <li>{@link #embed(Document)} → {@code "passage"} (called by VectorStore.add)</li>
 *   <li>{@link #embed(String)}   → {@code "query"}   (called by VectorStore.similaritySearch)</li>
 * </ul>
 */
@Slf4j
public class NvidiaEmbeddingModel implements EmbeddingModel {

    private final RestClient restClient;
    private final String model;
    private final int dims;

    public NvidiaEmbeddingModel(String baseUrl, String apiKey, String model, int dimensions) {
        this.model = model;
        this.dims = dimensions;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("Initialized NvidiaEmbeddingModel — model: {}, dimensions: {}", model, dimensions);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> inputs = request.getInstructions();

        // Determine input_type from EmbeddingOptions if provided,
        // otherwise default to "query" (search-time behavior).
        String inputType = "query";
        if (request.getOptions() instanceof NvidiaEmbeddingOptions opts) {
            inputType = opts.getInputType();
        }

        log.debug("NVIDIA embedding request — {} inputs, input_type: {}", inputs.size(), inputType);

        long startTime = System.currentTimeMillis();

        // Build request body with input_type
        Map<String, Object> body = new HashMap<>();
        body.put("input", inputs);
        body.put("model", model);
        body.put("input_type", inputType);
        body.put("encoding_format", "float");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/v1/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("⚡ NVIDIA embedding time: {}ms — {} inputs, input_type: {}", elapsed, inputs.size(), inputType);

        // Parse response
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        List<Embedding> embeddings = new ArrayList<>();
        for (Map<String, Object> item : data) {
            int index = ((Number) item.get("index")).intValue();
            @SuppressWarnings("unchecked")
            List<Number> embeddingValues = (List<Number>) item.get("embedding");
            float[] vector = new float[embeddingValues.size()];
            for (int i = 0; i < embeddingValues.size(); i++) {
                vector[i] = embeddingValues.get(i).floatValue();
            }
            embeddings.add(new Embedding(vector, index));
        }

        return new EmbeddingResponse(embeddings);
    }

    /**
     * Embed a document using input_type="passage".
     * Called by VectorStore.add() when storing documents.
     */
    @Override
    public float[] embed(Document document) {
        EmbeddingResponse response = call(
                new EmbeddingRequest(List.of(document.getText()), NvidiaEmbeddingOptions.PASSAGE));
        return response.getResult().getOutput();
    }

    /**
     * Embed a query string using input_type="query".
     * Called by VectorStore.similaritySearch() when searching.
     */
    @Override
    public float[] embed(String text) {
        EmbeddingResponse response = call(
                new EmbeddingRequest(List.of(text), NvidiaEmbeddingOptions.QUERY));
        return response.getResult().getOutput();
    }
}
