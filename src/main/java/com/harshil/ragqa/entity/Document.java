package com.harshil.ragqa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * IDEMPOTENCY NOTE: `contentHash` (SHA-256 of the raw document text) has a
 * unique constraint. If the same document is submitted twice — e.g. a
 * client retries an upload after a timeout, never knowing the first
 * request actually succeeded — we detect the duplicate by hash and return
 * the existing document instead of re-ingesting (re-chunking, re-embedding,
 * paying for OpenAI calls twice, and ending up with duplicate chunks in
 * the index). This is the same idempotency problem you'd solve with a
 * dedup key on an SQS consumer, just applied to an HTTP upload endpoint.
 */
@Entity
@Table(name = "documents", uniqueConstraints = @UniqueConstraint(columnNames = "content_hash"))
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionStatus status = IngestionStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private String errorMessage;

    public enum IngestionStatus {
        PENDING, CHUNKING, EMBEDDING, READY, FAILED
    }
}
