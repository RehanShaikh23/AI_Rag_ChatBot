package com.ragchatbot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback VectorStore configuration — uses in-memory SimpleVectorStore
 * when PGVector is not available (e.g., dev profile without PostgreSQL).
 */
@Configuration
@Slf4j
public class VectorStoreConfig {

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        log.error("🚨 PGVector NOT available — using in-memory SimpleVectorStore. " +
                 "Data will NOT persist across restarts. " +
                 "Metadata filter expressions (e.g. userId filter) will be IGNORED. " +
                 "To fix: run 'docker-compose up -d' in the backend directory.");
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
