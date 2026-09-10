package com.harshil.ragqa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Lob
    @Column(nullable = false)
    private String text;

    // Stored as a comma-separated string of floats for simplicity/portability
    // across H2 (local) and Postgres (prod) without a pgvector dependency.
    // At real scale you'd move this to pgvector, Pinecone, or a proper FAISS
    // deployment — see FlatVectorIndex for where that swap would happen.
    @Lob
    @Column(nullable = false)
    private String embeddingCsv;
}
