package com.harshil.ragqa.repository;

import com.harshil.ragqa.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByDocumentId(UUID documentId);
}
