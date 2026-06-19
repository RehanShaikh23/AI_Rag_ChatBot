package com.ragchatbot.repository;

import com.ragchatbot.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    Optional<Document> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Document.DocumentStatus status);
}
