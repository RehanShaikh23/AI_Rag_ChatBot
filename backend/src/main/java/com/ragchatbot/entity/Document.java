package com.ragchatbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Uploaded document metadata for RAG processing.
 * Actual vector embeddings are stored in the PGVector store.
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_user", columnList = "user_id"),
    @Index(name = "idx_document_status", columnList = "status")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Number of text chunks generated from this document.
     */
    @Column(name = "chunk_count")
    @Builder.Default
    private Integer chunkCount = 0;

    /**
     * Processing status: PENDING, PROCESSING, COMPLETED, FAILED.
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum DocumentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
