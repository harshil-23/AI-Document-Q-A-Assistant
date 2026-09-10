package com.harshil.ragqa.repository;

import com.harshil.ragqa.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findByContentHash(String contentHash);
}
